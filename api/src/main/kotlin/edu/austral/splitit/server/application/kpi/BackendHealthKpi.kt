package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.BackendHealthPort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** Text returned by the existing backend status check. */
@Component
@Order(BackendHealthKpi.ORDER)
class BackendHealthKpi(
    private val health: BackendHealthPort,
) : Kpi {
    override val id: String = KpiIds.BACKEND_HEALTH

    override fun calculate(context: KpiCalculationContext): KpiValue = KpiValue.Text(health.status())

    companion object {
        const val ORDER = 6
    }
}
