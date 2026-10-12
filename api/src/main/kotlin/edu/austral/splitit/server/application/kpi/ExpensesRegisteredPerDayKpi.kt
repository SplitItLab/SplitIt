package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.ExpenseRegistrationCount
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** Expenses whose createdAt falls inside the context window, inclusive. Zero expenses is 0.0. */
@Component
@Order(2)
class ExpensesRegisteredPerDayKpi(
    private val expenseRegistrationCount: ExpenseRegistrationCount,
) : Kpi {
    override val id: String = KpiIds.EXPENSES_REGISTERED_PER_DAY

    override fun calculate(context: KpiCalculationContext): KpiValue =
        KpiValue.Numeric(
            expenseRegistrationCount
                .countExpensesRegistered(context.from, context.to)
                .toDouble(),
        )
}
