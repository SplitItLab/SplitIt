package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class KpiEngine(
    private val kpis: List<Kpi>,
    private val lastRunPort: LastRunPort,
    private val clock: Clock,
) {
    fun calculate(context: KpiCalculationContext): List<KpiResult> {
        val ids = kpis.map { it.id }
        require(ids.size == ids.toSet().size) {
            "KPI ids must be unique: $ids"
        }

        val calculatedAt = clock.instant()
        val results = kpis.map { kpi -> calculateOne(kpi, context, calculatedAt) }
        lastRunPort.recordSuccessfulRun(calculatedAt)
        return results
    }

    @Suppress("TooGenericExceptionCaught") // un KPI no puede abortar la corrida (ADR SPT-77)
    private fun calculateOne(
        kpi: Kpi,
        context: KpiCalculationContext,
        calculatedAt: Instant,
    ): KpiResult {
        val value =
            try {
                kpi.calculate(context)
            } catch (exception: Exception) {
                logger.error("KPI calculation failed. id={}", kpi.id, exception)
                null
            }
        return KpiResult(id = kpi.id, value = value, calculatedAt = calculatedAt)
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(KpiEngine::class.java)
    }
}
