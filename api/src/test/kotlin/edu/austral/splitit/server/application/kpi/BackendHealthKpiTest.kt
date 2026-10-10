package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.BackendHealthPort
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BackendHealthKpiTest {
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T23:59:59Z"),
        )

    @Test
    fun `reports the status text`() {
        val kpi = BackendHealthKpi(BackendHealthPort { "OK" })

        assertEquals(KpiIds.BACKEND_HEALTH, kpi.id)
        assertEquals(KpiValue.Text("OK"), kpi.calculate(context))
    }
}
