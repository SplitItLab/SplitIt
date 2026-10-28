package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.kpi.KpiEndpoints
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaRequestMetrics::class)
@TestPropertySource(
    properties = [
        "POSTGRES_HOST=localhost",
        "POSTGRES_USER=test",
        "POSTGRES_PASSWORD=test",
        "JWT_SECRET=test-jwt-secret-that-is-long-enough",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
    ],
)
class JpaRequestMetricsTest(
    @Autowired private val metrics: JpaRequestMetrics,
    @Autowired private val samples: RequestSampleJpaRepository,
) {
    private val end = Instant.parse("2026-10-10T15:00:00Z")
    private val start = end.minus(Duration.ofDays(1))
    private val window = start..end

    @Test
    fun `p95 is the nearest rank of the window samples`() {
        // Rank ceil(0.95 * n). H2's percentile_disc answers 10, 18 and 5 for the 11, 19 and tied rows.
        val expectedByDurations =
            listOf(
                listOf(80L) to 80L,
                listOf(10L, 20L, 30L, 40L, 100L) to 100L,
                (1L..11L).toList() to 11L,
                (1L..19L).toList() to 19L,
                (1L..20L).toList() to 19L,
                (1L..21L).toList() to 20L,
                List(10) { 5L } + 7L to 7L,
            )

        expectedByDurations.forEachIndexed { index, (durations, expected) ->
            val endpoint = "GET /case/$index"
            durations.reversed().forEach { millis -> record(endpoint, 200, Duration.ofMillis(millis), end) }

            assertEquals(Duration.ofMillis(expected), metrics.p95(endpoint, window), "durations $durations")
        }
    }

    @Test
    fun `p95 reads only the requested endpoint inside the window`() {
        record(KpiEndpoints.GET_BALANCES, 200, Duration.ofMillis(10), start)
        (2..20L).forEach { step -> record(KpiEndpoints.GET_BALANCES, 200, Duration.ofMillis(step * 10), end) }
        record(KpiEndpoints.POST_EXPENSES, 201, Duration.ofSeconds(9), end)
        record(KpiEndpoints.GET_BALANCES, 200, Duration.ofSeconds(9), start.minus(ONE_MICROSECOND))
        record(KpiEndpoints.GET_BALANCES, 200, Duration.ofSeconds(9), end.plus(ONE_MICROSECOND))

        assertEquals(Duration.ofMillis(190), metrics.p95(KpiEndpoints.GET_BALANCES, window))
    }

    @Test
    fun `a 4xx response is not a server error but counts in the denominator`() {
        listOf(500, 599, 404, 499, 200, 200, 200, 200, 200, 200).forEach { status ->
            record("GET /api/status", status, Duration.ofMillis(1), end)
        }

        assertEquals(0.2, metrics.rate5xx(window))
    }

    @Test
    fun `the error rate ignores samples outside the window`() {
        record("GET /api/status", 500, Duration.ofMillis(1), end)
        record("GET /api/status", 200, Duration.ofMillis(1), end.plus(ONE_MICROSECOND))
        record("GET /api/status", 200, Duration.ofMillis(1), start.minus(ONE_MICROSECOND))

        assertEquals(1.0, metrics.rate5xx(window))
    }

    @Test
    fun `no samples in the window means no values`() {
        record(KpiEndpoints.POST_EXPENSES, 500, Duration.ofMillis(1), end.plus(ONE_MICROSECOND))

        assertNull(metrics.p95(KpiEndpoints.POST_EXPENSES, window))
        assertNull(metrics.rate5xx(window))
    }

    @Test
    fun `a new instance reads the same values from the database`() {
        record(KpiEndpoints.POST_EXPENSES, 201, Duration.ofMillis(40), end)
        record(KpiEndpoints.POST_EXPENSES, 500, Duration.ofMillis(900), end)

        val restarted = JpaRequestMetrics(samples)

        assertEquals(Duration.ofMillis(900), restarted.p95(KpiEndpoints.POST_EXPENSES, window))
        assertEquals(0.5, restarted.rate5xx(window))
    }

    @Test
    fun `deleting before a cutoff keeps the samples at or after it`() {
        val cutoff = end.minus(Duration.ofDays(7))
        record("GET /api/status", 200, Duration.ofMillis(1), cutoff.minus(ONE_MICROSECOND))
        record("GET /api/status", 500, Duration.ofMillis(1), cutoff)
        record("GET /api/status", 200, Duration.ofMillis(1), end)

        metrics.deleteRecordedBefore(cutoff)

        assertEquals(0.5, metrics.rate5xx(cutoff.minus(Duration.ofDays(1))..end))
    }

    @Test
    fun `a long request URI is stored truncated and still counts`() {
        record("GET /" + "a".repeat(300), 500, Duration.ofMillis(1), end)

        assertEquals(1.0, metrics.rate5xx(window))
    }

    private fun record(
        endpoint: String,
        statusCode: Int,
        duration: Duration,
        at: Instant,
    ) = metrics.record(RequestSample(endpoint, statusCode, duration, at))

    private companion object {
        val ONE_MICROSECOND: Duration = Duration.of(1, ChronoUnit.MICROS)
    }
}
