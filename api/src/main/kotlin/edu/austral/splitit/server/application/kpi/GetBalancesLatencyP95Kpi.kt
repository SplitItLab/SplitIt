package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.LatencyMetricsPort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** p95 latency of GET /api/events/{eventId}/balances in milliseconds. Null when the port has no samples. */
@Component
@Order(GetBalancesLatencyP95Kpi.ORDER)
class GetBalancesLatencyP95Kpi(
    private val latencyMetrics: LatencyMetricsPort,
) : Kpi {
    override val id: String = KpiIds.GET_BALANCES_LATENCY_P95

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val duration = latencyMetrics.p95(KpiEndpoints.GET_BALANCES, context.window()) ?: return null
        return KpiValue.Numeric(duration.toNanos().toDouble() / NANOS_PER_MILLISECOND)
    }

    companion object {
        const val ORDER = 10
        private const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
