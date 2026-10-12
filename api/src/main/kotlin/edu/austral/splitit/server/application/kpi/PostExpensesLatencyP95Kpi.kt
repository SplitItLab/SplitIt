package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.LatencyMetricsPort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** p95 latency of POST /api/events/{eventId}/expenses in milliseconds. Null when the port has no samples. */
@Component
@Order(PostExpensesLatencyP95Kpi.ORDER)
class PostExpensesLatencyP95Kpi(
    private val latencyMetrics: LatencyMetricsPort,
) : Kpi {
    override val id: String = KpiIds.POST_EXPENSES_LATENCY_P95

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val duration = latencyMetrics.p95(KpiEndpoints.POST_EXPENSES, context.window()) ?: return null
        return KpiValue.Numeric(duration.toNanos().toDouble() / NANOS_PER_MILLISECOND)
    }

    companion object {
        const val ORDER = 4
        private const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
