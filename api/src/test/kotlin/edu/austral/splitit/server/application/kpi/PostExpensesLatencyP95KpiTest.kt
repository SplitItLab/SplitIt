package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.LatencyMetricsPort
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostExpensesLatencyP95KpiTest {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")
    private val context = KpiCalculationContext(from, to)

    @Test
    fun `maps a millisecond duration to a numeric value`() {
        val kpi = PostExpensesLatencyP95Kpi(fixed(Duration.ofMillis(150)))

        assertEquals(KpiIds.POST_EXPENSES_LATENCY_P95, kpi.id)
        assertEquals(KpiValue.Numeric(150.0), kpi.calculate(context))
    }

    @Test
    fun `keeps sub millisecond precision`() {
        val kpi = PostExpensesLatencyP95Kpi(fixed(Duration.ofNanos(1_500_000)))

        assertEquals(KpiValue.Numeric(1.5), kpi.calculate(context))
    }

    @Test
    fun `returns null when the port has no samples`() {
        val kpi = PostExpensesLatencyP95Kpi(fixed(null))

        assertNull(kpi.calculate(context))
    }

    @Test
    fun `asks for the create expense endpoint and the context window`() {
        var endpoint: String? = null
        var window: ClosedRange<Instant>? = null
        val port =
            LatencyMetricsPort { requestedEndpoint, requestedWindow ->
                endpoint = requestedEndpoint
                window = requestedWindow
                Duration.ofMillis(80)
            }
        val kpi = PostExpensesLatencyP95Kpi(port)

        kpi.calculate(context)

        assertEquals(KpiEndpoints.POST_EXPENSES, endpoint)
        assertEquals(from..to, window)
    }

    private fun fixed(duration: Duration?): LatencyMetricsPort = LatencyMetricsPort { _, _ -> duration }
}
