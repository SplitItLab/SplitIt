package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.ExpenseNotFoundException
import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.AuthPrincipal
import edu.austral.splitit.server.domain.model.event.ExchangeQuote
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.ExpenseService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

data class CreateExpenseCommand(
    val principal: AuthPrincipal,
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
    val principal: AuthPrincipal,
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
    private val eventAccessService: EventAccessService,
    private val eventMemberService: EventMemberService,
    private val expenseService: ExpenseService,
    private val exchangeQuoteService: ExchangeQuoteService,
) {
    @Transactional(readOnly = true)
    fun quoteExpense(query: QuoteExpenseQuery): ExchangeQuote {
        val event = eventAccessService.findEventFor(query.principal, query.eventId)

        return exchangeQuoteService.quote(
            amount = query.amount,
            currency = query.currency,
            baseCurrency = event.baseCurrency,
        )
    }

    @Transactional
    fun addExpense(command: CreateExpenseCommand): ExpenseSummary {
        val event = eventAccessService.findEventFor(command.principal, command.eventId)

        // A guest only loads expenses paid by its own member.
        if (command.principal is AuthGuest && command.principal.memberId != command.paidByMemberId) {
            throw AccessDeniedException("Forbidden")
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
        val event = eventAccessService.findEventForMember(command.userId, command.eventId)

        val expense = expenseService.findById(command.expenseId)
        if (expense == null || expense.event.id != event.id) {
            throw ExpenseNotFoundException()
        }

        val paidByMember = eventMemberService.findById(command.paidByMemberId)
        require(paidByMember != null && paidByMember.event.id == event.id) {
            "Paying member must belong to the requested event"
        }

        val exchangeRate =
            if (expense.isInCurrency(command.currency)) {
                expense.exchangeRate
            } else {
                exchangeQuoteService
                    .quote(
                        amount = command.amount,
                        currency = command.currency,
                        baseCurrency = event.baseCurrency,
                    ).exchangeRate
            }

        val updated =
            expenseService.updateExpense(
                expense = expense,
                paidByMember = paidByMember,
                name = command.name,
                amount = command.amount,
                currency = command.currency,
                exchangeRate = exchangeRate,
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

    @Transactional
    fun deleteExpense(
        userId: Long,
        eventId: Long,
        expenseId: Long,
    ) {
        val event = eventAccessService.findEventForMember(userId, eventId)

        val expense = expenseService.findById(expenseId)
        if (expense == null || expense.event.id != event.id) {
            throw ExpenseNotFoundException()
        }

        expenseService.deleteExpense(expense)
    }

    @Transactional(readOnly = true)
    fun listExpenses(
        principal: AuthPrincipal,
        eventId: Long,
    ): List<ExpenseSummary> {
        val event = eventAccessService.findEventFor(principal, eventId)

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
}
