package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import org.springframework.stereotype.Service

@Service
class EventTransferService(
    private val eventRepository: EventRepository,
    private val eventMemberRepository: EventMemberRepository,
) {
    // Expenses hang from the members, so they move along with the memberships.
    fun transferAll(
        from: User,
        to: User,
    ) {
        eventRepository.transferOwnership(from, to)
        eventMemberRepository.transferMemberships(from, to)
    }
}
