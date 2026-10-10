package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KpiEngineTest {
    private val calculatedAt = Instant.parse("2026-10-10T15:00:00Z")
    private val clock = Clock.fixed(calculatedAt, ZoneOffset.UTC)
    private val context =
        KpiCalculationContext(
            from = Instant.parse("2026-10-10T00:00:00Z"),
            to = Instant.parse("2026-10-10T23:59:59Z"),
        )

    @Test
    fun `results keep input order and share the clock instant`() {
        val lastRun = RecordingLastRun()
        val engine =
            KpiEngine(
                kpis = listOf(fixed("first", 1.0), fixed("second", 2.0)),
                lastRunPort = lastRun,
                clock = clock,
            )

        val results = engine.calculate(context)

        assertEquals(listOf("first", "second"), results.map { it.id })
        assertEquals(KpiValue.Numeric(1.0), results[0].value)
        assertEquals(KpiValue.Numeric(2.0), results[1].value)
        assertTrue(results.all { it.calculatedAt == calculatedAt })
        assertEquals(calculatedAt, lastRun.recorded)
    }

    @Test
    fun `a null value and a thrown exception stay in place`() {
        val calls = mutableListOf<String>()
        val engine =
            KpiEngine(
                kpis =
                    listOf(
                        tracking("ok", calls) { KpiValue.Numeric(1.0) },
                        tracking("missing", calls) { null },
                        tracking("broken", calls) { error("boom") },
                        tracking("after", calls) { KpiValue.Text("still") },
                    ),
                lastRunPort = RecordingLastRun(),
                clock = clock,
            )

        val results = engine.calculate(context)

        assertEquals(listOf("ok", "missing", "broken", "after"), calls)
        assertEquals(KpiValue.Numeric(1.0), results[0].value)
        assertNull(results[1].value)
        assertEquals("broken", results[2].id)
        assertNull(results[2].value)
        assertEquals(KpiValue.Text("still"), results[3].value)
    }

    @Test
    fun `duplicate ids fail before calculation and do not record a run`() {
        val lastRun = RecordingLastRun()
        var calculated = false
        val engine =
            KpiEngine(
                kpis =
                    listOf(
                        tracking("same", mutableListOf()) {
                            calculated = true
                            KpiValue.Numeric(1.0)
                        },
                        fixed("same", 2.0),
                    ),
                lastRunPort = lastRun,
                clock = clock,
            )

        assertFailsWith<IllegalArgumentException> {
            engine.calculate(context)
        }
        assertEquals(false, calculated)
        assertNull(lastRun.recorded)
    }

    @Test
    fun `a failure while recording the run propagates`() {
        val lastRun = RecordingLastRun().also { it.failOnRecord = true }
        val engine = KpiEngine(kpis = listOf(fixed("only", 1.0)), lastRunPort = lastRun, clock = clock)

        assertFailsWith<IllegalStateException> {
            engine.calculate(context)
        }
    }

    @Test
    fun `an empty kpi list records the run and returns no results`() {
        val lastRun = RecordingLastRun()
        val engine = KpiEngine(kpis = emptyList(), lastRunPort = lastRun, clock = clock)

        assertEquals(emptyList(), engine.calculate(context))
        assertEquals(calculatedAt, lastRun.recorded)
    }

    private fun fixed(
        id: String,
        amount: Double,
    ): Kpi = tracking(id, mutableListOf()) { KpiValue.Numeric(amount) }

    private fun tracking(
        id: String,
        calls: MutableList<String>,
        body: () -> KpiValue?,
    ): Kpi =
        object : Kpi {
            override val id: String = id

            override fun calculate(context: KpiCalculationContext): KpiValue? {
                calls.add(id)
                return body()
            }
        }

    private class RecordingLastRun : LastRunPort {
        var recorded: Instant? = null
        var failOnRecord = false

        override fun recordSuccessfulRun(at: Instant) {
            if (failOnRecord) error("store failed")
            recorded = at
        }

        override fun lastSuccessfulRun(): Instant? = recorded
    }
}
