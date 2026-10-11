package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** ISO-8601 instant of the previous successful engine run. Null when the engine has never finished a run. */
@Component
@Order(LastSuccessfulRunKpi.ORDER)
class LastSuccessfulRunKpi(
    private val lastRun: LastRunPort,
) : Kpi {
    override val id: String = KpiIds.LAST_SUCCESSFUL_RUN

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val previous = lastRun.lastSuccessfulRun() ?: return null
        return KpiValue.Text(previous.toString())
    }

    companion object {
        const val ORDER = 7
    }
}
