package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.AuthPrincipal
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service

@Service
class EventAccessService(
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
) {
    // The owner and the members with an account reach the event.
    fun findEventForMember(
        userId: Long,
        eventId: Long,
    ): Event {
        val event = findEvent(eventId)

        val isMember = event.owner.id == userId || eventMemberService.isUserMemberOfEvent(eventId, userId)
        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        return event
    }

    // Also lets in a guest, but only to the event its credential was issued for.
    fun findEventFor(
        principal: AuthPrincipal,
        eventId: Long,
    ): Event =
        when (principal) {
            is AuthUser -> findEventForMember(principal.id, eventId)
            is AuthGuest -> findEventForGuest(principal, eventId)
        }

    private fun findEventForGuest(
        guest: AuthGuest,
        eventId: Long,
    ): Event {
        if (guest.eventId != eventId) {
            throw AccessDeniedException("Forbidden")
        }

        return findEvent(eventId)
    }

    private fun findEvent(eventId: Long): Event = eventService.findById(eventId) ?: throw EventNotFoundException()
}
