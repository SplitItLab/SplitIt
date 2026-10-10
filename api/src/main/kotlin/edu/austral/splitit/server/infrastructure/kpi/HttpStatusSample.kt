package edu.austral.splitit.server.infrastructure.kpi

import java.time.Instant

data class HttpStatusSample(
    val statusCode: Int,
    val at: Instant,
)
