package edu.austral.splitit.server.application.port

import edu.austral.splitit.server.application.service.KpiRunStatus
import java.time.Instant
import java.time.LocalDate

interface LastRunPort {
    fun recordRun(
        reportDay: LocalDate,
        ranAt: Instant,
        status: KpiRunStatus,
    )

    /** The latest run whose export was sent. Failed and skipped runs do not move it. */
    fun lastSuccessfulRun(): Instant?
}
