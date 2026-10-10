package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.ErrorRatePort
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ErrorRate5xxKpiTest {
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T15:00:00Z"),
        )

    @Test
    fun `returns the port rate for the 24 hours ending at context to`() {
        var window: ClosedRange<Instant>? = null
        val kpi =
            ErrorRate5xxKpi { requested ->
                window = requested
                0.25
            }

        assertEquals(KpiIds.ERROR_RATE_5XX_24H, kpi.id)
        assertEquals(KpiValue.Numeric(0.25), kpi.calculate(context))
        assertEquals(
            Instant.parse("2026-10-09T15:00:00Z")..Instant.parse("2026-10-10T15:00:00Z"),
            window,
        )
    }

    @Test
    fun `returns null when the port has no samples`() {
        val kpi = ErrorRate5xxKpi(ErrorRatePort { null })

        assertNull(kpi.calculate(context))
    }
}
