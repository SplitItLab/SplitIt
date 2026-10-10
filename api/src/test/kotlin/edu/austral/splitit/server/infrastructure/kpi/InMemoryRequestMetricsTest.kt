package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.kpi.KpiEndpoints
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryRequestMetricsTest {
    private val start = Instant.parse("2026-10-09T15:00:00Z")
    private val end = Instant.parse("2026-10-10T15:00:00Z")
    private val window = start..end
    private val clock = Clock.fixed(end, ZoneOffset.UTC)
    private val metrics = InMemoryRequestMetrics(clock)

    @Test
    fun `p95 of a known series is the nearest rank`() {
        listOf(10L, 20L, 30L, 40L, 100L).forEach { millis ->
            metrics.record(sample(KpiEndpoints.POST_EXPENSES, 201, Duration.ofMillis(millis), end))
        }

        assertEquals(Duration.ofMillis(100), metrics.p95(KpiEndpoints.POST_EXPENSES, window))
    }

    @Test
    fun `a single sample is the p95`() {
        metrics.record(sample(KpiEndpoints.POST_EXPENSES, 201, Duration.ofMillis(80), end))

        assertEquals(Duration.ofMillis(80), metrics.p95(KpiEndpoints.POST_EXPENSES, window))
    }

    @Test
    fun `p95 ignores other endpoints and samples outside the window`() {
        metrics.record(sample(KpiEndpoints.POST_EXPENSES, 201, Duration.ofMillis(10), end))
        metrics.record(sample("GET /api/status", 200, Duration.ofMillis(999), end))
        metrics.record(sample(KpiEndpoints.POST_EXPENSES, 201, Duration.ofMillis(999), end.plusNanos(1)))

        assertEquals(Duration.ofMillis(10), metrics.p95(KpiEndpoints.POST_EXPENSES, window))
    }

    @Test
    fun `p95 is null when the endpoint has no samples in the window`() {
        assertNull(metrics.p95(KpiEndpoints.POST_EXPENSES, window))
    }

    @Test
    fun `a 4xx response is not a 5xx error and still counts in the denominator`() {
        listOf(500, 404, 200, 200).forEach { status ->
            metrics.record(sample("GET /api/status", status, Duration.ofMillis(1), end))
        }

        assertEquals(0.25, metrics.rate5xx(window))
    }

    @Test
    fun `status 499 is not a server error`() {
        listOf(500, 404, 499, 200, 200).forEach { status ->
            metrics.record(sample("GET /api/status", status, Duration.ofMillis(1), end))
        }

        assertEquals(0.2, metrics.rate5xx(window))
    }

    @Test
    fun `a sample outside the window is ignored by the error rate`() {
        metrics.record(sample("GET /api/status", 500, Duration.ofMillis(1), end))
        metrics.record(sample("GET /api/status", 500, Duration.ofMillis(1), end.plusNanos(1)))

        assertEquals(1.0, metrics.rate5xx(window))
    }

    @Test
    fun `no samples in the window returns null`() {
        assertNull(metrics.rate5xx(window))
        metrics.record(sample("GET /api/status", 500, Duration.ofMillis(1), end.plusNanos(1)))
        assertNull(metrics.rate5xx(window))
    }

    @Test
    fun `samples older than the retention are dropped`() {
        val freshClock = Clock.fixed(end, ZoneOffset.UTC)
        val store = InMemoryRequestMetrics(freshClock)
        store.record(sample("GET /api/status", 500, Duration.ofMillis(1), end.minus(Duration.ofHours(26))))
        store.record(sample("GET /api/status", 200, Duration.ofMillis(1), end.minus(Duration.ofHours(1))))

        val wide = end.minus(Duration.ofDays(3))..end
        assertEquals(0.0, store.rate5xx(wide))
    }

    private fun sample(
        endpoint: String,
        statusCode: Int,
        duration: Duration,
        at: Instant,
    ) = RequestSample(endpoint, statusCode, duration, at)
}
