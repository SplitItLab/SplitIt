package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.LatencyMetricsPort
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class EmptyLatencyMetrics : LatencyMetricsPort {
    override fun p95(
        endpoint: String,
        window: ClosedRange<Instant>,
    ): Duration? = null
}
