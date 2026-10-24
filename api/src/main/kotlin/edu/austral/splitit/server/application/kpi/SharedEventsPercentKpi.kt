package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.application.port.SharedEventCount
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Percentage of the events created inside the context window that have an invite link, from 0 to 100.
 * Null when no event was created.
 */
@Component
@Order(SharedEventsPercentKpi.ORDER)
class SharedEventsPercentKpi(
    private val eventCreationCount: EventCreationCount,
    private val sharedEventCount: SharedEventCount,
) : Kpi {
    override val id: String = KpiIds.SHARED_EVENTS_PERCENT

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val total = eventCreationCount.countEventsCreated(context.from, context.to)
        if (total == 0L) {
            return null
        }
        val shared = sharedEventCount.countSharedEventsCreated(context.from, context.to)
        return KpiValue.Numeric(shared.toDouble() / total.toDouble() * PERCENT)
    }

    companion object {
        const val ORDER = 9
        private const val PERCENT = 100.0
    }
}
