package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastSuccessfulRunKpiTest {
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T23:59:59Z"),
        )
    private val lastRun = mock<LastRunPort>()
    private val kpi = LastSuccessfulRunKpi(lastRun)

    @Test
    fun `the value is the instant of the last sent run`() {
        whenever(lastRun.lastSuccessfulRun()).thenReturn(Instant.parse("2026-10-10T03:15:00Z"))

        assertEquals(KpiIds.LAST_SUCCESSFUL_RUN, kpi.id)
        assertEquals(KpiValue.Text("2026-10-10T03:15:00Z"), kpi.calculate(context))
    }

    @Test
    fun `the value is null until a run is sent`() {
        assertNull(kpi.calculate(context))
    }
}
