package edu.austral.splitit.server.domain.kpi

import java.time.Instant

data class KpiResult(
    val id: String,
    val value: KpiValue?,
    val calculatedAt: Instant,
)
