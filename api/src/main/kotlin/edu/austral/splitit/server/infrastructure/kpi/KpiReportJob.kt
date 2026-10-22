package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.service.RunKpiReport
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.persistence.JpaRequestMetrics
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

@Component
class KpiReportJob(
    private val runKpiReport: RunKpiReport,
    private val requestMetrics: JpaRequestMetrics,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${kpi.report.cron}", zone = "\${kpi.report.zone}")
    fun runDaily() {
        val report = runCatching { report() }
        // Cleanup runs even after a failed report; the report failure stays the one thrown.
        runCatching { deleteExpiredSamples() }.onFailure { cleanupFailure ->
            report.exceptionOrNull()?.addSuppressed(cleanupFailure) ?: throw cleanupFailure
        }
        report.getOrThrow()
    }

    private fun report() {
        val receipt = runKpiReport.run()
        logger.info(
            "KPI run stored. destination={} reportDay={} id={} kpiCount={}",
            receipt.destination,
            receipt.reportDay,
            receipt.id,
            receipt.results.size,
        )
        receipt.results.forEach { result ->
            logger.info("KPI value. id={} value={}", result.id, result.loggedValue())
        }
    }

    private fun deleteExpiredSamples() {
        requestMetrics.deleteRecordedBefore(clock.instant().minus(Duration.ofDays(SAMPLE_RETENTION_DAYS)))
    }

    private companion object {
        const val SAMPLE_RETENTION_DAYS = 7L
        val logger: Logger = LoggerFactory.getLogger(KpiReportJob::class.java)
    }
}

private fun KpiResult.loggedValue(): String =
    when (val stored = value) {
        null -> "null"
        is KpiValue.Numeric -> stored.amount.toString()
        is KpiValue.Text -> stored.text
    }
