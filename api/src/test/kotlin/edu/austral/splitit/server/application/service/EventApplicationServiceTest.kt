package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    private val user = Helpers.user(name = "Mateo", email = "mateo@example.com", passwordHash = "hash")

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
    fun `createEvent rejects blank participant names before persisting`() {
        whenever(userService.getById(1L)).thenReturn(user)

        val command =
            CreateEventCommand(
                userId = 1L,
                name = "Viaje",
                baseCurrency = "ARS",
                participantNames = listOf("Ana", "   "),
            )

        assertFailsWith<IllegalArgumentException> {
            eventApplicationService.createEvent(command)
        }

        verify(eventService, never()).save(
            owner = any(),
            name = any(),
            description = anyOrNull(),
            iconKey = anyOrNull(),
            baseCurrency = any(),
        )
        verify(eventMemberService, never()).addMember(any(), any(), anyOrNull())
        verify(eventMemberService, never()).addMembers(any(), any())
    }

    @Test
    fun `createEvent rejects duplicate participant names before persisting`() {
        whenever(userService.getById(1L)).thenReturn(user)

        val command =
            CreateEventCommand(
                userId = 1L,
                name = "Viaje",
                baseCurrency = "ARS",
                participantNames = listOf("Ana", " ana "),
            )

        assertFailsWith<IllegalArgumentException> {
            eventApplicationService.createEvent(command)
        }

        verify(eventService, never()).save(
            owner = any(),
            name = any(),
            description = anyOrNull(),
            iconKey = anyOrNull(),
            baseCurrency = any(),
        )
        verify(eventMemberService, never()).addMember(any(), any(), anyOrNull())
        verify(eventMemberService, never()).addMembers(any(), any())
    }

    @Test
    fun `createEvent rejects participant name that matches owner before persisting`() {
        whenever(userService.getById(1L)).thenReturn(user)

        val command =
            CreateEventCommand(
                userId = 1L,
                name = "Viaje",
                baseCurrency = "ARS",
                participantNames = listOf("mateo"),
            )

        assertFailsWith<IllegalArgumentException> {
            eventApplicationService.createEvent(command)
        }

        verify(eventService, never()).save(
            owner = any(),
            name = any(),
            description = anyOrNull(),
            iconKey = anyOrNull(),
            baseCurrency = any(),
        )
        verify(eventMemberService, never()).addMember(any(), any(), anyOrNull())
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

    @Test
    fun `getEventById returns event detail when requested by owner`() {
        val event =
            Event
                .create(
                    owner = user,
                    name = "Viaje a Bariloche",
                    description = "Vacaciones de verano",
                    iconKey = "plane",
                    baseCurrency = "ARS",
                ).apply { id = 10L }

        val member1 = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val member2 = EventMember.create(event, "Ana", null).apply { id = 101L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findByEventId(10L)).thenReturn(listOf(member1, member2))

        val detail = eventApplicationService.getEventById(userId = 1L, eventId = 10L)

        assertEquals(10L, detail.id)
        assertEquals("Viaje a Bariloche", detail.name)
        assertEquals("Vacaciones de verano", detail.description)
        assertEquals("plane", detail.iconKey)
        assertEquals("ARS", detail.baseCurrency)
        assertEquals(2L, detail.memberCount)
        assertEquals(2, detail.members.size)
        assertEquals(detail.memberCount, detail.members.size.toLong())

        val first = detail.members[0]
        assertEquals(100L, first.id)
        assertEquals("Mateo", first.name)
        assertEquals("mateo@example.com", first.email)
        assertFalse(first.isGuest)

        val second = detail.members[1]
        assertEquals(101L, second.id)
        assertEquals("Ana", second.name)
        assertNull(second.email)
        assertTrue(second.isGuest)

        verify(eventService).findById(10L)
        verify(eventMemberService).findByEventId(10L)
    }

    @Test
    fun `getEventById returns event detail when requested by linked member`() {
        val otherOwner =
            Helpers
                .user(name = "Otro", email = "otro@example.com", passwordHash = "hash")
                .apply { id = 2L }
        val event =
            Event
                .create(
                    owner = otherOwner,
                    name = "Asado",
                    baseCurrency = "ARS",
                ).apply { id = 20L }

        val member = EventMember.create(event, "Mateo", user).apply { id = 200L }

        whenever(eventService.findById(20L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(20L, 1L)).thenReturn(true)
        whenever(eventMemberService.findByEventId(20L)).thenReturn(listOf(member))

        val detail = eventApplicationService.getEventById(userId = 1L, eventId = 20L)

        assertEquals(20L, detail.id)
        assertEquals("Asado", detail.name)
        assertEquals(1L, detail.memberCount)
        assertEquals(1, detail.members.size)
        assertEquals(detail.memberCount, detail.members.size.toLong())
        assertEquals("Mateo", detail.members[0].name)
        assertEquals("mateo@example.com", detail.members[0].email)
        assertFalse(detail.members[0].isGuest)
    }

    @Test
    fun `getEventById throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            eventApplicationService.getEventById(userId = 1L, eventId = 999L)
        }

        verify(eventMemberService, never()).findByEventId(any())
    }

    @Test
    fun `getEventById throws AccessDeniedException when user is not owner and not member`() {
        val otherOwner =
            Helpers
                .user(name = "Otro", email = "otro@example.com", passwordHash = "hash")
                .apply { id = 2L }
        val event =
            Event
                .create(
                    owner = otherOwner,
                    name = "Privado",
                    baseCurrency = "ARS",
                ).apply { id = 30L }

        whenever(eventService.findById(30L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(30L, 1L)).thenReturn(false)

        assertFailsWith<AccessDeniedException> {
            eventApplicationService.getEventById(userId = 1L, eventId = 30L)
        }

        verify(eventMemberService, never()).findByEventId(any())
    }
}
