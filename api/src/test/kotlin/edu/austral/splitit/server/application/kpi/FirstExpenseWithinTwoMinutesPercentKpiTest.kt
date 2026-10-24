package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.FirstExpenseDelay
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirstExpenseWithinTwoMinutesPercentKpiTest {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")
    private val context = KpiCalculationContext(from, to)

    @Test
    fun `percentage is undefined when no event was created`() {
        val kpi = FirstExpenseWithinTwoMinutesPercentKpi(delays())

        assertEquals(KpiIds.EVENTS_WITH_EXPENSE_IN_2_MIN_PERCENT, kpi.id)
        assertNull(kpi.calculate(context))
    }

    @Test
    fun `exactly two minutes counts and one second more does not`() {
        val kpi =
            FirstExpenseWithinTwoMinutesPercentKpi(
                delays(Duration.ofMinutes(2), Duration.ofMinutes(2).plusSeconds(1)),
            )

        assertEquals(KpiValue.Numeric(50.0), kpi.calculate(context))
    }

    @Test
    fun `an event without expenses counts only in the total`() {
        val kpi = FirstExpenseWithinTwoMinutesPercentKpi(delays(Duration.ofSeconds(30), null, null, null))

        assertEquals(KpiValue.Numeric(25.0), kpi.calculate(context))
    }

    @Test
    fun `asks for the events created inside the context window`() {
        var window: Pair<Instant, Instant>? = null
        val kpi =
            FirstExpenseWithinTwoMinutesPercentKpi { requestedFrom, requestedTo ->
                window = requestedFrom to requestedTo
                emptyList()
            }

        kpi.calculate(context)

        assertEquals(from to to, window)
    }

    private fun delays(vararg delays: Duration?): FirstExpenseDelay = FirstExpenseDelay { _, _ -> delays.toList() }
}
