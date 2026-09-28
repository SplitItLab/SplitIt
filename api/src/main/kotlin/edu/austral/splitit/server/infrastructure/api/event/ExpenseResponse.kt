package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.service.ExpenseSummary
import java.math.BigDecimal
import java.time.LocalDate

data class ExpensePayerResponse(
    val id: Long,
    val name: String,
)

data class ExpenseResponse(
    val id: Long,
    val eventId: Long,
    val name: String,
    val originalAmount: BigDecimal,
    val originalCurrency: String,
    val baseAmount: BigDecimal,
    val baseCurrency: String,
    val paidByMember: ExpensePayerResponse,
    val expenseDate: LocalDate,
) {
    companion object {
        fun of(summary: ExpenseSummary): ExpenseResponse =
            ExpenseResponse(
                id = summary.id,
                eventId = summary.eventId,
                name = summary.name,
                originalAmount = summary.originalAmount,
                originalCurrency = summary.originalCurrency,
                baseAmount = summary.baseAmount,
                baseCurrency = summary.baseCurrency,
                paidByMember =
                    ExpensePayerResponse(
                        id = summary.paidByMember.id,
                        name = summary.paidByMember.name,
                    ),
                expenseDate = summary.expenseDate,
            )
    }
}
