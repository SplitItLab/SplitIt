package edu.austral.splitit.server.infrastructure.kpi

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertNull

class EmptyLatencyMetricsTest {
    @Test
    fun `returns null for any endpoint and window`() {
        val metrics = EmptyLatencyMetrics()
        val window = Instant.parse("2026-10-10T00:00:00Z")..Instant.parse("2026-10-10T23:59:59Z")

        assertNull(metrics.p95("POST /api/events/{eventId}/expenses", window))
        assertNull(metrics.p95("GET /api/status", window))
    }
}
