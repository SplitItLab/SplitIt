package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.ErrorRatePort
import edu.austral.splitit.server.application.port.LatencyMetricsPort
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.ceil

/** Timestamped request samples. Micrometer timers are not windowed, so the KPI ports read this store. */
@Component
class InMemoryRequestMetrics(
    private val clock: Clock,
) : LatencyMetricsPort,
    ErrorRatePort {
    private val samples = ConcurrentLinkedQueue<RequestSample>()

    fun record(sample: RequestSample) {
        val cutoff = clock.instant().minus(RETENTION)
        samples.removeIf { it.at.isBefore(cutoff) }
        if (!sample.at.isBefore(cutoff)) {
            samples.add(sample)
        }
    }

    override fun p95(
        endpoint: String,
        window: ClosedRange<Instant>,
    ): Duration? {
        val durations =
            snapshot()
                .filter { it.endpoint == endpoint && it.at in window }
                .map { it.duration }
                .sorted()
        if (durations.isEmpty()) {
            return null
        }
        val index = ceil(PERCENTILE * durations.size).toInt() - 1
        return durations[index]
    }

    override fun rate5xx(window: ClosedRange<Instant>): Double? {
        val inWindow = snapshot().filter { it.at in window }
        if (inWindow.isEmpty()) {
            return null
        }
        val serverErrors = inWindow.count { it.statusCode in SERVER_ERROR }
        return serverErrors.toDouble() / inWindow.size.toDouble()
    }

    private fun snapshot(): List<RequestSample> = samples.toList()

    private companion object {
        const val PERCENTILE = 0.95
        const val RETENTION_HOURS = 25L
        val RETENTION: Duration = Duration.ofHours(RETENTION_HOURS)
        const val SERVER_ERROR_MIN = 500
        const val SERVER_ERROR_MAX = 599
        val SERVER_ERROR: IntRange = SERVER_ERROR_MIN..SERVER_ERROR_MAX
    }
}
