package edu.austral.splitit.server.application.port

import java.time.Duration
import java.time.Instant

fun interface LatencyMetricsPort {
    fun p95(
        endpoint: String,
        window: ClosedRange<Instant>,
    ): Duration?
}
