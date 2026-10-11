package edu.austral.splitit.server.domain.kpi

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KpiCalculationContextTest {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")

    @Test
    fun `window includes both bounds`() {
        val context = KpiCalculationContext(from, to)

        assertTrue(from in context.window())
        assertTrue(to in context.window())
        assertEquals(from..to, context.window())
    }

    @Test
    fun `equal bounds are a valid instant window`() {
        val context = KpiCalculationContext(from, from)

        assertEquals(from..from, context.window())
    }

    @Test
    fun `end before start is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            KpiCalculationContext(to, from)
        }
    }
}
