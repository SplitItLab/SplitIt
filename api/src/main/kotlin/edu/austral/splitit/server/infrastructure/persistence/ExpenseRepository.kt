package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findAllByEventId(eventId: Long): List<Expense>

    @Query(
        """
        SELECT e FROM Expense e
        WHERE e.event.id = :eventId
        ORDER BY e.expenseDate DESC, e.id DESC
        """,
    )
    fun findAllOrderedByEventId(
        @Param("eventId") eventId: Long,
    ): List<Expense>

    fun findAllByPaidByMemberId(paidByMemberId: Long): List<Expense>
}
