package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.ErrorRatePort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Fraction of responses that are 5xx during the 24 hours ending at context.to.
 * 4xx is not an error. Null when there are no samples.
 */
@Component
@Order(ErrorRate5xxKpi.ORDER)
class ErrorRate5xxKpi(
    private val errorRate: ErrorRatePort,
) : Kpi {
    override val id: String = KpiIds.ERROR_RATE_5XX_24H

    override fun calculate(context: KpiCalculationContext): KpiValue? {
        val end = context.to
        val window = end.minus(Duration.ofHours(WINDOW_HOURS))..end
        val rate = errorRate.rate5xx(window) ?: return null
        return KpiValue.Numeric(rate)
    }

    companion object {
        const val ORDER = 5
        private const val WINDOW_HOURS = 24L
    }
}
