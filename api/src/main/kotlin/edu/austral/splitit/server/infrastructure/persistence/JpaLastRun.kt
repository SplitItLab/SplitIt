package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.application.service.KpiRunStatus
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate

@Repository
class JpaLastRun(
    private val runs: KpiRunJpaRepository,
) : LastRunPort {
    override fun recordRun(
        reportDay: LocalDate,
        ranAt: Instant,
        status: KpiRunStatus,
    ) {
        runs.save(KpiRunEntity(reportDay = reportDay, ranAt = ranAt, status = status.column()))
    }

    override fun lastSuccessfulRun(): Instant? = runs.findLastRanAt(KpiRunStatus.SENT.column())
}

// kpi_run.status only accepts lowercase values.
private fun KpiRunStatus.column(): String = name.lowercase()
