package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.exception.ExpenseNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.ExchangeQuote
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.ExpenseService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

data class CreateExpenseCommand(
    val userId: Long,
    val eventId: Long,
    val name: String,
    val amount: BigDecimal,
    val currency: String,
    val paidByMemberId: Long,
)

data class UpdateExpenseCommand(
    val userId: Long,
    val eventId: Long,
    val expenseId: Long,
    val name: String,
    val amount: BigDecimal,
    val currency: String,
    val paidByMemberId: Long,
)

data class QuoteExpenseQuery(
    val userId: Long,
    val eventId: Long,
    val amount: BigDecimal,
    val currency: String,
)

data class ExpensePayerSummary(
    val id: Long,
    val name: String,
)

data class ExpenseSummary(
    val id: Long,
    val eventId: Long,
    val name: String,
    val originalAmount: BigDecimal,
    val originalCurrency: String,
    val exchangeRate: BigDecimal,
    val baseAmount: BigDecimal,
    val baseCurrency: String,
    val paidByMember: ExpensePayerSummary,
    val expenseDate: LocalDate,
)

@Service
class ExpenseApplicationService(
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
    private val expenseService: ExpenseService,
    private val exchangeQuoteService: ExchangeQuoteService,
) {
    @Transactional(readOnly = true)
    fun quoteExpense(query: QuoteExpenseQuery): ExchangeQuote {
        val event = findEventForMember(query.userId, query.eventId)

        return exchangeQuoteService.quote(
            amount = query.amount,
            currency = query.currency,
            baseCurrency = event.baseCurrency,
        )
    }

    @Transactional
    fun addExpense(command: CreateExpenseCommand): ExpenseSummary {
        val event = findEventForMember(command.userId, command.eventId)

        val paidByMember = eventMemberService.findById(command.paidByMemberId)
        require(paidByMember != null && paidByMember.event.id == event.id) {
            "Paying member must belong to the requested event"
        }

        val quote =
            exchangeQuoteService.quote(
                amount = command.amount,
                currency = command.currency,
                baseCurrency = event.baseCurrency,
            )

        val expense =
            expenseService.addExpense(
                event = event,
                paidByMember = paidByMember,
                name = command.name,
                amount = quote.originalAmount,
                currency = quote.originalCurrency.get(),
                exchangeRate = quote.exchangeRate,
            )

        return ExpenseSummary(
            id = requireNotNull(expense.id),
            eventId = requireNotNull(event.id),
            name = expense.name,
            originalAmount = expense.originalAmount,
            originalCurrency = expense.originalCurrency,
            exchangeRate = expense.exchangeRate,
            baseAmount = expense.baseAmount,
            baseCurrency = event.baseCurrency,
            paidByMember =
                ExpensePayerSummary(
                    id = requireNotNull(paidByMember.id),
                    name = paidByMember.displayName,
                ),
            expenseDate = expense.expenseDate,
        )
    }

    @Transactional
    fun updateExpense(command: UpdateExpenseCommand): ExpenseSummary {
        val event = findEventForMember(command.userId, command.eventId)

        val expense = expenseService.findById(command.expenseId)
        if (expense == null || expense.event.id != event.id) {
            throw ExpenseNotFoundException()
        }

        val paidByMember = eventMemberService.findById(command.paidByMemberId)
        require(paidByMember != null && paidByMember.event.id == event.id) {
            "Paying member must belong to the requested event"
        }

        val quote =
            exchangeQuoteService.quote(
                amount = command.amount,
                currency = command.currency,
                baseCurrency = event.baseCurrency,
            )

        val updated =
            expenseService.updateExpense(
                expense = expense,
                paidByMember = paidByMember,
                name = command.name,
                amount = quote.originalAmount,
                currency = quote.originalCurrency.get(),
                exchangeRate = quote.exchangeRate,
            )

        return ExpenseSummary(
            id = requireNotNull(updated.id),
            eventId = requireNotNull(event.id),
            name = updated.name,
            originalAmount = updated.originalAmount,
            originalCurrency = updated.originalCurrency,
            exchangeRate = updated.exchangeRate,
            baseAmount = updated.baseAmount,
            baseCurrency = event.baseCurrency,
            paidByMember =
                ExpensePayerSummary(
                    id = requireNotNull(paidByMember.id),
                    name = paidByMember.displayName,
                ),
            expenseDate = updated.expenseDate,
        )
    }

    @Transactional(readOnly = true)
    fun listExpenses(
        userId: Long,
        eventId: Long,
    ): List<ExpenseSummary> {
        val event = findEventForMember(userId, eventId)

        return expenseService.findByEventId(eventId).map { expense ->
            ExpenseSummary(
                id = requireNotNull(expense.id),
                eventId = requireNotNull(event.id),
                name = expense.name,
                originalAmount = expense.originalAmount,
                originalCurrency = expense.originalCurrency,
                exchangeRate = expense.exchangeRate,
                baseAmount = expense.baseAmount,
                baseCurrency = event.baseCurrency,
                paidByMember =
                    ExpensePayerSummary(
                        id = requireNotNull(expense.paidByMember.id),
                        name = expense.paidByMember.displayName,
                    ),
                expenseDate = expense.expenseDate,
            )
        }
    }

    private fun findEventForMember(
        userId: Long,
        eventId: Long,
    ): Event {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        val isOwner = event.owner.id == userId
        val isMember = isOwner || eventMemberService.isUserMemberOfEvent(eventId, userId)
        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        return event
    }
}
