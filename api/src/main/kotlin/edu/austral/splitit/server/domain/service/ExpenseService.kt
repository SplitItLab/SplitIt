package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.infrastructure.persistence.ExpenseRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/** Position of the last expense already seen; the next page starts right after it. */
data class ExpenseCursor(
    val date: LocalDate,
    val id: Long,
)

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
        exchangeRate: BigDecimal,
    ): Expense {
        val expense =
            Expense.create(
                event = event,
                paidByMember = paidByMember,
                name = name,
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = exchangeRate,
                expenseDate = LocalDate.now(),
            )
        return expenseRepository.save(expense)
    }

    fun updateExpense(
        expense: Expense,
        paidByMember: EventMember,
        name: String,
        amount: BigDecimal,
        currency: String,
        exchangeRate: BigDecimal,
    ): Expense {
        val updated =
            expense.update(
                paidByMember = paidByMember,
                name = name,
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = exchangeRate,
            )
        return expenseRepository.save(updated)
    }

    fun deleteExpense(expense: Expense) {
        expenseRepository.delete(expense)
    }

    fun findById(id: Long): Expense? = expenseRepository.findByIdOrNull(id)

    fun findByEventId(eventId: Long): List<Expense> = expenseRepository.findAllOrderedByEventId(eventId)

    /** Newest first, at most [size] expenses strictly after [after] in that order. */
    fun findPageByEventId(
        eventId: Long,
        size: Int,
        after: ExpenseCursor?,
    ): List<Expense> {
        val pageable = PageRequest.of(0, size)
        return if (after == null) {
            expenseRepository.findFirstPage(eventId, pageable)
        } else {
            expenseRepository.findPageBefore(eventId, after.date, after.id, pageable)
        }
    }

    fun countByEventId(eventId: Long): Long = expenseRepository.countByEventId(eventId)

    fun totalBaseAmountByEventId(eventId: Long): BigDecimal = expenseRepository.sumBaseAmountByEventId(eventId)
}
