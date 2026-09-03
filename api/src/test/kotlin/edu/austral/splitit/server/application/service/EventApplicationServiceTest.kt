package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class EventApplicationServiceTest {
    private val userService: UserService = mock()
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()

    private val eventApplicationService =
        EventApplicationService(
            userService = userService,
            eventService = eventService,
            eventMemberService = eventMemberService,
        )

    private val user = User(id = 1L, name = "Mateo", email = "mateo@example.com", passwordHash = "hash")

    @Test
    fun `createEvent saves event, owner member, and participants`() {
        whenever(userService.getById(1L)).thenReturn(user)

        val createdEvent =
            Event
                .create(
                    owner = user,
                    name = "Viaje a Bariloche",
                    description = "Vacaciones de verano",
                    iconKey = "plane",
                    baseCurrency = "ARS",
                ).apply { id = 10L }

        whenever(
            eventService.save(
                owner = eq(user),
                name = eq("Viaje a Bariloche"),
                description = eq("Vacaciones de verano"),
                iconKey = eq("plane"),
                baseCurrency = eq("ARS"),
            ),
        ).thenReturn(createdEvent)

        val ownerMember = EventMember.create(createdEvent, "Mateo", user).apply { id = 100L }
        whenever(eventMemberService.addMember(createdEvent, "Mateo", user)).thenReturn(ownerMember)

        val command =
            CreateEventCommand(
                userId = 1L,
                name = "Viaje a Bariloche",
                description = "Vacaciones de verano",
                iconKey = "plane",
                baseCurrency = "ARS",
                participantNames = listOf("Ana", "Juan"),
            )

        val summary = eventApplicationService.createEvent(command)

        assertEquals(10L, summary.id)
        assertEquals("Viaje a Bariloche", summary.name)
        assertEquals("Vacaciones de verano", summary.description)
        assertEquals("plane", summary.iconKey)
        assertEquals("ARS", summary.baseCurrency)
        assertEquals(3L, summary.memberCount)

        verify(eventMemberService).addMember(createdEvent, "Mateo", user)
        verify(eventMemberService).addMembers(createdEvent, listOf("Ana", "Juan"))
    }

    @Test
    fun `createEvent with empty participants only adds owner`() {
        whenever(userService.getById(1L)).thenReturn(user)

        val createdEvent =
            Event
                .create(
                    owner = user,
                    name = "Asado",
                    baseCurrency = "ARS",
                ).apply { id = 20L }

        whenever(
            eventService.save(
                owner = eq(user),
                name = eq("Asado"),
                description = eq(null),
                iconKey = eq(null),
                baseCurrency = eq("ARS"),
            ),
        ).thenReturn(createdEvent)

        val ownerMember = EventMember.create(createdEvent, "Mateo", user).apply { id = 200L }
        whenever(eventMemberService.addMember(createdEvent, "Mateo", user)).thenReturn(ownerMember)

        val command =
            CreateEventCommand(
                userId = 1L,
                name = "Asado",
                baseCurrency = "ARS",
                participantNames = emptyList(),
            )

        val summary = eventApplicationService.createEvent(command)

        assertEquals(20L, summary.id)
        assertEquals(1L, summary.memberCount)

        verify(eventMemberService).addMember(createdEvent, "Mateo", user)
        verify(eventMemberService, never()).addMembers(any(), any())
    }

    @Test
    fun `listUserEvents returns events with member counts`() {
        val event1 = Event.create(user, "Evento 1", baseCurrency = "ARS").apply { id = 1L }
        val event2 = Event.create(user, "Evento 2", baseCurrency = "USD").apply { id = 2L }

        whenever(eventService.findUserEvents(1L)).thenReturn(listOf(event1, event2))
        whenever(eventMemberService.getMemberCounts(listOf(1L, 2L))).thenReturn(mapOf(1L to 4L, 2L to 2L))

        val result = eventApplicationService.listUserEvents(1L)

        assertEquals(2, result.size)
        assertEquals("Evento 1", result[0].name)
        assertEquals(4L, result[0].memberCount)
        assertEquals("Evento 2", result[1].name)
        assertEquals(2L, result[1].memberCount)
    }

    @Test
    fun `listUserEvents returns empty list when user has no events`() {
        whenever(eventService.findUserEvents(1L)).thenReturn(emptyList())

        val result = eventApplicationService.listUserEvents(1L)

        assertEquals(emptyList(), result)
        verify(eventMemberService, never()).getMemberCounts(any())
    }
}
