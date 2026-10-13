package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.kpi.KpiEndpoints
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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
        val clock = MutableClock(end)
        val store = InMemoryRequestMetrics(clock)
        repeat(3) {
            store.record(sample("GET /api/status", 500, Duration.ofMillis(1), end))
        }
        clock.advance(Duration.ofHours(49))
        store.record(sample("GET /api/status", 200, Duration.ofMillis(1), clock.instant()))

        val wide = end.minus(Duration.ofDays(1))..clock.instant()
        assertEquals(0.0, store.rate5xx(wide))
    }

    @Test
    fun `a sample exactly at the retention cutoff is kept`() {
        val cutoff = end.minus(Duration.ofHours(48))
        metrics.record(sample("GET /api/status", 500, Duration.ofMillis(1), cutoff))
        metrics.record(sample("GET /api/status", 200, Duration.ofMillis(1), end))

        val wide = end.minus(Duration.ofDays(3))..end
        assertEquals(0.5, metrics.rate5xx(wide))
    }

    @Test
    fun `a sample one nanosecond before the cutoff is dropped`() {
        val sampleTime = end.minus(Duration.ofHours(48))
        val clock = MutableClock(sampleTime.plus(Duration.ofHours(48)).minusNanos(1))
        val store = InMemoryRequestMetrics(clock)
        store.record(sample("GET /api/status", 500, Duration.ofMillis(1), sampleTime))
        clock.advance(Duration.ofNanos(2))
        store.record(sample("GET /api/status", 200, Duration.ofMillis(1), clock.instant()))

        val wide = sampleTime.minus(Duration.ofDays(1))..clock.instant()
        assertEquals(0.0, store.rate5xx(wide))
    }

    @Test
    fun `only expired samples leave the window empty`() {
        val clock = MutableClock(end)
        val store = InMemoryRequestMetrics(clock)
        store.record(sample("GET /api/status", 500, Duration.ofMillis(1), end))
        clock.advance(Duration.ofHours(49))
        store.record(sample("GET /api/status", 500, Duration.ofMillis(1), clock.instant().minus(Duration.ofHours(49))))

        val wide = end.minus(Duration.ofDays(1))..clock.instant()
        assertNull(store.rate5xx(wide))
    }

    @Test
    fun `a sample from the start of the previous day is still there at the daily report`() {
        val dayStart = Instant.parse("2026-10-12T00:00:00Z")
        val reportAt = Instant.parse("2026-10-13T03:15:00Z")
        val clock = MutableClock(dayStart)
        val store = InMemoryRequestMetrics(clock)
        store.record(sample(KpiEndpoints.POST_EXPENSES, 500, Duration.ofMillis(40), dayStart))

        clock.set(reportAt)
        store.record(sample("GET /api/status", 200, Duration.ofMillis(1), reportAt))

        val reportedDay = dayStart..Instant.parse("2026-10-13T00:00:00Z").minusNanos(1_000)
        assertEquals(1.0, store.rate5xx(reportedDay))
        assertEquals(Duration.ofMillis(40), store.p95(KpiEndpoints.POST_EXPENSES, reportedDay))
    }

    private fun sample(
        endpoint: String,
        statusCode: Int,
        duration: Duration,
        at: Instant,
    ) = RequestSample(endpoint, statusCode, duration, at)

    private class MutableClock(
        private var now: Instant,
    ) : Clock() {
        fun set(instant: Instant) {
            now = instant
        }

        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }
}
