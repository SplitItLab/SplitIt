package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.LatencyMetricsPort
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.kpi.InMemoryRequestMetrics
import edu.austral.splitit.server.infrastructure.kpi.RequestSample
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetBalancesLatencyP95KpiTest {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")
    private val context = KpiCalculationContext(from, to)

    @Test
    fun `maps a millisecond duration to a numeric value`() {
        val kpi = GetBalancesLatencyP95Kpi(fixed(Duration.ofMillis(150)))

        assertEquals(KpiIds.GET_BALANCES_LATENCY_P95, kpi.id)
        assertEquals(KpiValue.Numeric(150.0), kpi.calculate(context))
    }

    @Test
    fun `keeps sub millisecond precision`() {
        val kpi = GetBalancesLatencyP95Kpi(fixed(Duration.ofNanos(1_500_000)))

        assertEquals(KpiValue.Numeric(1.5), kpi.calculate(context))
    }

    @Test
    fun `returns null when the port has no samples`() {
        val kpi = GetBalancesLatencyP95Kpi(fixed(null))

        assertNull(kpi.calculate(context))
    }

    @Test
    fun `asks for the balances endpoint and the context window`() {
        var endpoint: String? = null
        var window: ClosedRange<Instant>? = null
        val port =
            LatencyMetricsPort { requestedEndpoint, requestedWindow ->
                endpoint = requestedEndpoint
                window = requestedWindow
                Duration.ofMillis(80)
            }
        val kpi = GetBalancesLatencyP95Kpi(port)

        kpi.calculate(context)

        assertEquals("GET /api/events/{eventId}/balances", endpoint)
        assertEquals(from..to, window)
    }

    @Test
    fun `reads only the balances samples of the window`() {
        val at = Instant.parse("2026-10-10T12:00:00Z")
        val metrics = InMemoryRequestMetrics(Clock.fixed(at, ZoneOffset.UTC))
        (1..20L).forEach { millis ->
            metrics.record(RequestSample(KpiEndpoints.GET_BALANCES, 200, Duration.ofMillis(millis * 10), at))
        }
        metrics.record(RequestSample(KpiEndpoints.POST_EXPENSES, 201, Duration.ofSeconds(9), at))
        metrics.record(RequestSample(KpiEndpoints.GET_BALANCES, 200, Duration.ofSeconds(9), from.minusSeconds(1)))

        val kpi = GetBalancesLatencyP95Kpi(metrics)

        assertEquals(KpiValue.Numeric(190.0), kpi.calculate(context))
    }

    private fun fixed(duration: Duration?): LatencyMetricsPort = LatencyMetricsPort { _, _ -> duration }
}
