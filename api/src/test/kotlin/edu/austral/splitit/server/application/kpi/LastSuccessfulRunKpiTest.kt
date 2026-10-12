package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.service.KpiEngine
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.kpi.InMemoryLastRun
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastSuccessfulRunKpiTest {
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T23:59:59Z"),
        )
    private val first = Instant.parse("2026-10-10T15:00:00Z")
    private val second = Instant.parse("2026-10-11T15:00:00Z")

    @Test
    fun `the value is the previous successful run`() {
        val lastRun = InMemoryLastRun()
        val kpi = LastSuccessfulRunKpi(lastRun)

        val firstResult =
            KpiEngine(
                kpis = listOf(kpi),
                lastRunPort = lastRun,
                clock = Clock.fixed(first, ZoneOffset.UTC),
            ).calculate(context)

        assertEquals(KpiIds.LAST_SUCCESSFUL_RUN, kpi.id)
        assertNull(firstResult.single().value)
        assertEquals(first, lastRun.lastSuccessfulRun())

        val secondResult =
            KpiEngine(
                kpis = listOf(kpi),
                lastRunPort = lastRun,
                clock = Clock.fixed(second, ZoneOffset.UTC),
            ).calculate(context)

        assertEquals(KpiValue.Text(first.toString()), secondResult.single().value)
        assertEquals(second, lastRun.lastSuccessfulRun())
    }
}
