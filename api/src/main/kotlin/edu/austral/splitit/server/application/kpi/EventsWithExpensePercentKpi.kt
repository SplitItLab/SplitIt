package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.EventExpenseCoverage
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** Historical percentage of events with at least one expense, from 0 to 100. Null when there are no events. */
@Component
@Order(EventsWithExpensePercentKpi.ORDER)
class EventsWithExpensePercentKpi(
    private val coverage: EventExpenseCoverage,
) : Kpi {
    override val id: String = KpiIds.EVENTS_WITH_EXPENSE_PERCENT

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val total = coverage.countEvents()
        if (total == 0L) {
            return null
        }
        val withExpense = coverage.countEventsWithAtLeastOneExpense()
        return KpiValue.Numeric(withExpense.toDouble() / total.toDouble() * PERCENT)
    }

    companion object {
        const val ORDER = 3
        private const val PERCENT = 100.0
    }
}
