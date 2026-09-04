package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.Expense
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findAllByEventId(eventId: Long): List<Expense>

    fun findAllByPaidByMemberId(paidByMemberId: Long): List<Expense>
}
