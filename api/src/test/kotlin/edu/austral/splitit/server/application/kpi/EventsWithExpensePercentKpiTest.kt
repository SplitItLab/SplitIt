package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.EventExpenseCoverage
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventsWithExpensePercentKpiTest {
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T23:59:59Z"),
        )

    @Test
    fun `percentage is undefined when there are no events`() {
        val kpi = EventsWithExpensePercentKpi(coverage(events = 0, withExpense = 0))

        assertNull(kpi.calculate(context))
    }

    @Test
    fun `percentage is zero when no event has an expense`() {
        val kpi = EventsWithExpensePercentKpi(coverage(events = 4, withExpense = 0))

        assertEquals(KpiValue.Numeric(0.0), kpi.calculate(context))
    }

    private fun coverage(
        events: Long,
        withExpense: Long,
    ): EventExpenseCoverage =
        object : EventExpenseCoverage {
            override fun countEventsWithAtLeastOneExpense(): Long = withExpense

            override fun countEvents(): Long = events
        }
}
