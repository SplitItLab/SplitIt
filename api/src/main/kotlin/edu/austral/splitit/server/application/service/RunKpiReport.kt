package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class RunKpiReport(
    private val engine: KpiEngine,
    private val exporter: KpiExporter,
    private val clock: Clock,
) {
    fun run(reportDay: LocalDate? = null): KpiRunReceipt {
        val day = reportDay ?: LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1)
        val from = day.atStartOfDay(ZoneOffset.UTC).toInstant()
        val to =
            day
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .minusNanos(1)
        val results = engine.calculate(KpiCalculationContext(from, to))
        return exporter.export(
            KpiRun(
                reportDay = day,
                windowFrom = from,
                windowTo = to,
                results = results,
            ),
        )
    }

    fun list(): List<KpiRunReceipt> = exporter.list()
}
