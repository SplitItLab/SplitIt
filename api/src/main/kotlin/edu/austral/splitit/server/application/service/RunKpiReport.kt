package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class RunKpiReport(
    private val engine: KpiEngine,
    private val exporter: KpiExporter,
    private val lastRun: LastRunPort,
    private val clock: Clock,
    private val transactions: TransactionOperations,
) {
    fun run(reportDay: LocalDate? = null): KpiRunReceipt {
        val day = reportDay ?: LocalDate.now(clock.withZone(REPORT_ZONE)).minusDays(1)
        val from = day.atStartOfDay(REPORT_ZONE).toInstant()
        val to =
            day
                .plusDays(1)
                .atStartOfDay(REPORT_ZONE)
                .toInstant()
                // TIMESTAMP(6) rounds 1 ns before midnight up to the next day.
                .minusNanos(1_000)
        // Outside the transaction: a failed KPI query would abort it and lose the whole run (ADR SPT-77).
        val results = engine.calculate(KpiCalculationContext(from, to))
        // The stored snapshot and its kpi_run row commit together or not at all.
        return transactions.execute {
            val receipt =
                exporter.export(
                    KpiRun(
                        reportDay = day,
                        windowFrom = from,
                        windowTo = to,
                        results = results,
                    ),
                )
            // Recorded after the calculation, so last_successful_run reports the previous sent run.
            lastRun.recordRun(day, clock.instant(), receipt.status)
            receipt
        }
    }

    fun list(): List<KpiRunReceipt> = exporter.list()

    private companion object {
        // The central platform measures each report day in Buenos Aires time.
        val REPORT_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }
}
