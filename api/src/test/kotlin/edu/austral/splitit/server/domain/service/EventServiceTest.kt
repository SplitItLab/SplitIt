package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EventServiceTest {
    private val eventRepository: EventRepository = mock()
    private val eventService = EventService(eventRepository)

    private val owner = User(id = 1L, name = "Dueño", email = "dueno@example.com", passwordHash = "hash")

    @Test
    fun `save creates and persists event`() {
        whenever(eventRepository.save(any<Event>())).thenAnswer { invocation ->
            val e = invocation.getArgument<Event>(0)
            e.id = 10L
            e
        }

        val event =
            eventService.save(
                owner = owner,
                name = "Viaje a Bariloche",
                description = "Vacaciones",
                iconKey = "plane",
                baseCurrency = "ARS",
            )

        assertEquals(10L, event.id)
        assertEquals("Viaje a Bariloche", event.name)
        assertEquals("ARS", event.baseCurrency)
        verify(eventRepository).save(any<Event>())
    }

    @Test
    fun `findById returns event when present`() {
        val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")
        event.id = 1L
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event))

        val result = eventService.findById(1L)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `findById returns null when missing`() {
        whenever(eventRepository.findById(99L)).thenReturn(Optional.empty())

        val result = eventService.findById(99L)
        assertNull(result)
    }

    @Test
    fun `getById throws EventNotFoundException when missing`() {
        whenever(eventRepository.findById(99L)).thenReturn(Optional.empty())

        assertFailsWith<EventNotFoundException> {
            eventService.getById(99L)
        }
    }

    @Test
    fun `findUserEvents delegates to repository`() {
        val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")
        whenever(eventRepository.findDistinctByOwnerIdOrMemberUserId(1L)).thenReturn(listOf(event))

        val result = eventService.findUserEvents(1L)
        assertEquals(1, result.size)
        assertEquals("Viaje", result[0].name)
    }
}
