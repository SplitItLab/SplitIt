package edu.austral.splitit.server.infrastructure.kpi

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryErrorRateTest {
    private val start = Instant.parse("2026-10-09T15:00:00Z")
    private val end = Instant.parse("2026-10-10T15:00:00Z")
    private val window = start..end

    @Test
    fun `a 4xx response is not a 5xx error and still counts in the denominator`() {
        val rate =
            InMemoryErrorRate(
                listOf(
                    sample(500, end),
                    sample(404, end),
                    sample(200, end),
                    sample(200, start),
                ),
            )

        assertEquals(0.25, rate.rate5xx(window))
    }

    @Test
    fun `status 499 is not a server error`() {
        val rate =
            InMemoryErrorRate(
                listOf(
                    sample(500, end),
                    sample(404, end),
                    sample(499, end),
                    sample(200, end),
                    sample(200, start),
                ),
            )

        assertEquals(0.2, rate.rate5xx(window))
    }

    @Test
    fun `a sample outside the window is ignored`() {
        val rate =
            InMemoryErrorRate(
                listOf(
                    sample(500, end),
                    sample(500, end.plusNanos(1)),
                ),
            )

        assertEquals(1.0, rate.rate5xx(window))
    }

    @Test
    fun `no samples in the window returns null`() {
        assertNull(InMemoryErrorRate().rate5xx(window))
        assertNull(
            InMemoryErrorRate(listOf(sample(500, end.plusNanos(1)))).rate5xx(window),
        )
    }

    private fun sample(
        statusCode: Int,
        at: Instant,
    ) = HttpStatusSample(statusCode, at)
}
