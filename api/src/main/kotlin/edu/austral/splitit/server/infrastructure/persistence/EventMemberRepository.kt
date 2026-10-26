package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.EventMember
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventMemberRepository : JpaRepository<EventMember, Long> {
    fun countByEventId(eventId: Long): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT em FROM EventMember em WHERE em.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): EventMember?

    @Query(
        """
        SELECT em FROM EventMember em
        LEFT JOIN FETCH em.user
        WHERE em.event.id = :eventId
        ORDER BY em.id ASC
        """,
    )
    fun findAllByEventId(
        @Param("eventId") eventId: Long,
    ): List<EventMember>

    fun findByEventIdAndUserId(
        eventId: Long,
        userId: Long,
    ): EventMember?

    fun existsByEventIdAndUserId(
        eventId: Long,
        userId: Long,
    ): Boolean

    fun deleteAllByEventId(eventId: Long)

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
