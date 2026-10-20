package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

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

    @Query("SELECT COUNT(x) FROM Expense x WHERE x.event.id = :eventId")
    fun countExpensesByEventId(
        @Param("eventId") eventId: Long,
    ): Long

    fun countByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
        from: Instant,
        to: Instant,
    ): Long

    @Query(
        """
        SELECT COUNT(e) FROM Event e
        WHERE EXISTS (
            SELECT 1 FROM Expense x
            WHERE x.event.id = e.id
        )
        """,
    )
    fun countWithAtLeastOneExpense(): Long

    @Query(
        """
        SELECT e.createdAt AS createdAt, MIN(x.createdAt) AS firstExpenseAt
        FROM Event e LEFT JOIN Expense x ON x.event = e
        WHERE e.createdAt >= :from AND e.createdAt <= :to
        GROUP BY e.id, e.createdAt
        """,
    )
    fun findFirstExpenseOfEventsCreatedBetween(
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): List<EventFirstExpense>

    @Query(
        """
        SELECT COUNT(e) FROM Event e
        WHERE e.createdAt >= :from AND e.createdAt <= :to
          AND EXISTS (
            SELECT 1 FROM InviteLink l
            WHERE l.event.id = e.id
          )
        """,
    )
    fun countCreatedBetweenWithInviteLink(
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): Long
}

interface EventFirstExpense {
    val createdAt: Instant
    val firstExpenseAt: Instant?
}
