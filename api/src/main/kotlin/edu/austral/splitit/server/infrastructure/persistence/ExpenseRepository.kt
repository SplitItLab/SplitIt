package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.Expense
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

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

    @Query(
        """
        SELECT e FROM Expense e JOIN FETCH e.paidByMember
        WHERE e.event.id = :eventId
        ORDER BY e.expenseDate DESC, e.id DESC
        """,
    )
    fun findFirstPage(
        @Param("eventId") eventId: Long,
        pageable: Pageable,
    ): List<Expense>

    @Query(
        """
        SELECT e FROM Expense e JOIN FETCH e.paidByMember
        WHERE e.event.id = :eventId
          AND (e.expenseDate < :beforeDate OR (e.expenseDate = :beforeDate AND e.id < :beforeId))
        ORDER BY e.expenseDate DESC, e.id DESC
        """,
    )
    fun findPageBefore(
        @Param("eventId") eventId: Long,
        @Param("beforeDate") beforeDate: LocalDate,
        @Param("beforeId") beforeId: Long,
        pageable: Pageable,
    ): List<Expense>

    @Query("SELECT COALESCE(SUM(e.baseAmount), 0) FROM Expense e WHERE e.event.id = :eventId")
    fun sumBaseAmountByEventId(
        @Param("eventId") eventId: Long,
    ): BigDecimal

    fun countByEventId(eventId: Long): Long

    fun findAllByPaidByMemberId(paidByMemberId: Long): List<Expense>

    fun countByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
        from: Instant,
        to: Instant,
    ): Long
}
