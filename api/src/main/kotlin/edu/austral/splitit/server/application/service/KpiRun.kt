package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.domain.kpi.KpiResult
import java.time.Instant
import java.time.LocalDate

data class KpiRun(
    val reportDay: LocalDate,
    val windowFrom: Instant,
    val windowTo: Instant,
    val results: List<KpiResult>,
)

/** What happened to a run at its destination. Only a sent run moves last_successful_run. */
enum class KpiRunStatus {
    SENT,
    FAILED,
    SKIPPED,
}

data class KpiRunReceipt(
    val id: Long,
    val destination: String,
    val status: KpiRunStatus,
    val reportDay: LocalDate,
    val windowFrom: Instant,
    val windowTo: Instant,
    val calculatedAt: Instant,
    val results: List<KpiResult>,
)
