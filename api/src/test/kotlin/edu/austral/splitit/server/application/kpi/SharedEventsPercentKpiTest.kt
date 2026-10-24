package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.application.port.SharedEventCount
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedEventsPercentKpiTest {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")
    private val context = KpiCalculationContext(from, to)

    @Test
    fun `percentage is undefined when no event was created`() {
        val kpi = kpi(created = 0, shared = 0)

        assertEquals(KpiIds.SHARED_EVENTS_PERCENT, kpi.id)
        assertNull(kpi.calculate(context))
    }

    @Test
    fun `percentage is zero when no created event was shared`() {
        assertEquals(KpiValue.Numeric(0.0), kpi(created = 3, shared = 0).calculate(context))
    }

    @Test
    fun `percentage of the created events that were shared`() {
        assertEquals(KpiValue.Numeric(25.0), kpi(created = 4, shared = 1).calculate(context))
    }

    @Test
    fun `both counts use the context window`() {
        val windows = mutableListOf<Pair<Instant, Instant>>()
        val kpi =
            SharedEventsPercentKpi(
                EventCreationCount { requestedFrom, requestedTo ->
                    windows += requestedFrom to requestedTo
                    2
                },
                SharedEventCount { requestedFrom, requestedTo ->
                    windows += requestedFrom to requestedTo
                    1
                },
            )

        kpi.calculate(context)

        assertEquals(listOf(from to to, from to to), windows)
    }

    private fun kpi(
        created: Long,
        shared: Long,
    ) = SharedEventsPercentKpi(EventCreationCount { _, _ -> created }, SharedEventCount { _, _ -> shared })
}
