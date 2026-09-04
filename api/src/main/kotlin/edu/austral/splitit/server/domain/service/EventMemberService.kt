package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import org.springframework.stereotype.Service

@Service
class EventMemberService(
    private val eventMemberRepository: EventMemberRepository,
) {
    fun addMember(
        event: Event,
        displayName: String,
        user: User? = null,
    ): EventMember {
        val member =
            EventMember.create(
                event = event,
                displayName = displayName,
                user = user,
            )
        return eventMemberRepository.save(member)
    }

    fun addMembers(
        event: Event,
        displayNames: List<String>,
    ): List<EventMember> {
        val members =
            displayNames.map { name ->
                EventMember.create(
                    event = event,
                    displayName = name,
                    user = null,
                )
            }
        return eventMemberRepository.saveAll(members)
    }

    fun countByEventId(eventId: Long): Long = eventMemberRepository.countByEventId(eventId)

    fun findByEventId(eventId: Long): List<EventMember> = eventMemberRepository.findAllByEventId(eventId)

    fun getMemberCounts(eventIds: Collection<Long>): Map<Long, Long> {
        if (eventIds.isEmpty()) return emptyMap()
        return eventMemberRepository
            .countMembersByEventIds(eventIds)
            .associate { it.eventId to it.memberCount }
    }
}
