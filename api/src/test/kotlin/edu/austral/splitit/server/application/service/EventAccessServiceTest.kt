package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventAccessServiceTest {
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()

    private val eventAccessService = EventAccessService(eventService, eventMemberService)

    private val owner = Helpers.user(name = "Mateo", email = "mateo@example.com", id = 1L)
    private val event =
        Event
            .create(owner = owner, name = "Viaje a Bariloche", baseCurrency = "ARS")
            .apply { id = 10L }

    @Test
    fun `the owner reaches the event without checking membership`() {
        whenever(eventService.findById(10L)).thenReturn(event)

        assertEquals(event, eventAccessService.findEventFor(Helpers.authUser(1L), 10L))
        verify(eventMemberService, never()).isUserMemberOfEvent(any(), any())
    }

    @Test
    fun `a member with an account reaches the event`() {
        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(true)

        assertEquals(event, eventAccessService.findEventFor(Helpers.authUser(2L), 10L))
    }

    @Test
    fun `a user outside the event is forbidden`() {
        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(false)

        assertFailsWith<AccessDeniedException> {
            eventAccessService.findEventFor(Helpers.authUser(2L), 10L)
        }
    }

    @Test
    fun `a guest reaches the event of its credential`() {
        whenever(eventService.findById(10L)).thenReturn(event)

        assertEquals(event, eventAccessService.findEventFor(guestOf(eventId = 10L), 10L))
    }

    @Test
    fun `a guest is forbidden from any other event`() {
        assertFailsWith<AccessDeniedException> {
            eventAccessService.findEventFor(guestOf(eventId = 10L), 20L)
        }

        verifyNoInteractions(eventService, eventMemberService)
    }

    @Test
    fun `a guest of an event that no longer exists gets not found`() {
        whenever(eventService.findById(10L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            eventAccessService.findEventFor(guestOf(eventId = 10L), 10L)
        }
    }

    private fun guestOf(eventId: Long) = AuthGuest(eventId = eventId, memberId = 30L, name = "Juan")
}
