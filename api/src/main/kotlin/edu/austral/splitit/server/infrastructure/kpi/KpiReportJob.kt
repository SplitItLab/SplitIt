package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.service.RunKpiReport
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KpiReportJob(
    private val runKpiReport: RunKpiReport,
) {
    @Scheduled(cron = "\${kpi.report.cron}", zone = "\${kpi.report.zone}")
    fun runDaily() {
        val receipt = runKpiReport.run()
        logger.info(
            "KPI run stored. destination={} delivered={} reportDay={} id={} kpiCount={}",
            receipt.destination,
            receipt.delivered,
            receipt.reportDay,
            receipt.id,
            receipt.results.size,
        )
        receipt.results.forEach { result ->
            logger.info("KPI value. id={} value={}", result.id, result.loggedValue())
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(KpiReportJob::class.java)
    }
}

private fun KpiResult.loggedValue(): String =
    when (val stored = value) {
        null -> "null"
        is KpiValue.Numeric -> stored.amount.toString()
        is KpiValue.Text -> stored.text
    }
