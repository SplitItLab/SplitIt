package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.infrastructure.persistence.ExpenseRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
) {
    fun addExpense(
        event: Event,
        paidByMember: EventMember,
        name: String,
        amount: BigDecimal,
        currency: String,
    ): Expense {
        val expense =
            Expense.create(
                event = event,
                paidByMember = paidByMember,
                name = name,
                originalAmount = amount,
                originalCurrency = currency,
                expenseDate = LocalDate.now(),
            )
        return expenseRepository.save(expense)
    }

    fun findByEventId(eventId: Long): List<Expense> = expenseRepository.findAllOrderedByEventId(eventId)
}
