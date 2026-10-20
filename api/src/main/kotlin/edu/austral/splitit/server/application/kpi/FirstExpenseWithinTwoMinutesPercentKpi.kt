package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.FirstExpenseDelay
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Percentage of the events created inside the context window whose first expense came within 2 minutes,
 * from 0 to 100. Null when no event was created.
 */
@Component
@Order(FirstExpenseWithinTwoMinutesPercentKpi.ORDER)
class FirstExpenseWithinTwoMinutesPercentKpi(
    private val firstExpenseDelay: FirstExpenseDelay,
) : Kpi {
    override val id: String = KpiIds.EVENTS_WITH_EXPENSE_IN_2_MIN_PERCENT

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val delays = firstExpenseDelay.firstExpenseDelays(context.from, context.to)
        if (delays.isEmpty()) {
            return null
        }
        val quick = delays.count { it != null && it <= LIMIT }
        return KpiValue.Numeric(quick.toDouble() / delays.size.toDouble() * PERCENT)
    }

    companion object {
        const val ORDER = 8
        private const val PERCENT = 100.0
        private val LIMIT: Duration = Duration.ofMinutes(2)
    }
}
