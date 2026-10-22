package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.port.ErrorRatePort
import edu.austral.splitit.server.application.port.LatencyMetricsPort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Timestamped request samples. Micrometer timers are not windowed, so the KPI ports read this table. */
@Repository
class JpaRequestMetrics(
    private val samples: RequestSampleJpaRepository,
) : LatencyMetricsPort,
    ErrorRatePort {
    fun record(sample: RequestSample) {
        val micros = sample.duration.toNanos() / NANOS_PER_MICROSECOND
        samples.save(
            RequestSampleEntity(
                // A request without a route pattern keeps the raw URI, which the client controls.
                endpoint = sample.endpoint.take(RequestSampleEntity.ENDPOINT_LENGTH),
                statusCode = sample.statusCode.toShort(),
                // duration_us is an INTEGER: a request longer than ~35 minutes is stored at the cap.
                durationMicros = micros.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                recordedAt = sample.at,
            ),
        )
    }

    override fun p95(
        endpoint: String,
        window: ClosedRange<Instant>,
    ): Duration? =
        samples
            .p95DurationMicros(endpoint, window.start, window.endInclusive)
            ?.let { micros -> Duration.of(micros.toLong(), ChronoUnit.MICROS) }

    override fun rate5xx(window: ClosedRange<Instant>): Double? = samples.rate5xx(window.start, window.endInclusive)

    @Transactional
    fun deleteRecordedBefore(cutoff: Instant) {
        samples.deleteRecordedBefore(cutoff)
    }

    private companion object {
        const val NANOS_PER_MICROSECOND = 1_000L
    }
}
