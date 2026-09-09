package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.EventMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventMemberRepository : JpaRepository<EventMember, Long> {
    fun countByEventId(eventId: Long): Long

    fun findAllByEventId(eventId: Long): List<EventMember>

    @Query(
        """
        SELECT em.event.id AS eventId, COUNT(em.id) AS memberCount
        FROM EventMember em
        WHERE em.event.id IN :eventIds
        GROUP BY em.event.id
        """,
    )
    fun countMembersByEventIds(
        @Param("eventIds") eventIds: Collection<Long>,
    ): List<EventMemberCountProjection>
}

interface EventMemberCountProjection {
    val eventId: Long
    val memberCount: Long
}
