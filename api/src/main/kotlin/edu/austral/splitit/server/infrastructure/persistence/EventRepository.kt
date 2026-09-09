package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventRepository : JpaRepository<Event, Long> {
    @Query(
        """
        SELECT e FROM Event e
        WHERE e.owner.id = :userId
           OR e.id IN (SELECT em.event.id FROM EventMember em WHERE em.user.id = :userId)
        ORDER BY e.createdAt DESC
        """,
    )
    fun findDistinctByOwnerIdOrMemberUserId(
        @Param("userId") userId: Long,
    ): List<Event>
}
