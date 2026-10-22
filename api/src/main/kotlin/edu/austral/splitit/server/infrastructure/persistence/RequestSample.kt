package edu.austral.splitit.server.infrastructure.persistence

import java.time.Duration
import java.time.Instant

data class RequestSample(
    val endpoint: String,
    val statusCode: Int,
    val duration: Duration,
    val at: Instant,
)
