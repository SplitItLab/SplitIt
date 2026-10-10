package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.BackendHealthPort
import edu.austral.splitit.server.application.port.ErrorRatePort
import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.application.port.EventExpenseCoverage
import edu.austral.splitit.server.application.port.ExpenseRegistrationCount
import edu.austral.splitit.server.application.port.LatencyMetricsPort
import edu.austral.splitit.server.application.service.KpiEngine
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.kpi.InMemoryLastRun
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class EighthKpiExtensionTest {
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T23:59:59Z"),
        )
    private val clock = Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `adding an eighth kpi keeps the seven values`() {
        val seven = calculate(extra = null)
        val eight = calculate(extra = eighthKpi())

        assertEquals(seven.map { it.id to it.value }, eight.take(seven.size).map { it.id to it.value })
        assertEquals(eight.size, seven.size + 1)
        assertEquals("eighth-kpi", eight.last().id)
        assertEquals(KpiValue.Numeric(1.0), eight.last().value)
    }

    private fun calculate(extra: Kpi?): List<KpiResult> {
        val lastRun = InMemoryLastRun()
        val kpis = productionKpis(lastRun) + listOfNotNull(extra)
        return KpiEngine(kpis = kpis, lastRunPort = lastRun, clock = clock).calculate(context)
    }

    private fun productionKpis(lastRun: InMemoryLastRun): List<Kpi> =
        listOf(
            EventsCreatedPerDayKpi(EventCreationCount { _, _ -> 4 }),
            ExpensesRegisteredPerDayKpi(ExpenseRegistrationCount { _, _ -> 7 }),
            EventsWithExpensePercentKpi(
                object : EventExpenseCoverage {
                    override fun countEventsWithAtLeastOneExpense(): Long = 1

                    override fun countEvents(): Long = 4
                },
            ),
            PostExpensesLatencyP95Kpi(LatencyMetricsPort { _, _ -> Duration.ofMillis(80) }),
            ErrorRate5xxKpi(ErrorRatePort { 0.0 }),
            BackendHealthKpi(BackendHealthPort { "OK" }),
            LastSuccessfulRunKpi(lastRun),
        )

    private fun eighthKpi(): Kpi =
        object : Kpi {
            override val id: String = "eighth-kpi"

            override fun calculate(context: KpiCalculationContext): KpiValue = KpiValue.Numeric(1.0)
        }
}
