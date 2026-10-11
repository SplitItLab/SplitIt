package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class RunKpiReportTest {
    private val calculatedAt = Instant.parse("2026-10-12T03:15:00Z")
    private val clock = Clock.fixed(calculatedAt, ZoneOffset.UTC)
    private val engine = mock<KpiEngine>()
    private val exporter = RecordingExporter()
    private val service = RunKpiReport(engine, exporter, clock)

    @Test
    fun `run without a day uses yesterday UTC and returns the exporter receipt`() {
        whenever(engine.calculate(any())).thenReturn(results())

        val receipt = service.run()

        val context = argumentCaptor<KpiCalculationContext>()
        verify(engine).calculate(context.capture())
        assertEquals(Instant.parse("2026-10-11T00:00:00Z"), context.firstValue.from)
        assertEquals(Instant.parse("2026-10-11T23:59:59.999999999Z"), context.firstValue.to)

        val exported = exporter.exported.single()
        assertEquals(LocalDate.parse("2026-10-11"), exported.reportDay)
        assertEquals(context.firstValue.from, exported.windowFrom)
        assertEquals(context.firstValue.to, exported.windowTo)
        assertEquals(results(), exported.results)

        assertEquals(1L, receipt.id)
        assertEquals("local", receipt.destination)
        assertEquals(false, receipt.delivered)
        assertEquals(LocalDate.parse("2026-10-11"), receipt.reportDay)
        assertEquals(exported.windowFrom, receipt.windowFrom)
        assertEquals(exported.windowTo, receipt.windowTo)
        assertEquals(calculatedAt, receipt.calculatedAt)
        assertEquals(results(), receipt.results)
    }

    @Test
    fun `two runs on different days leave two records`() {
        whenever(engine.calculate(any())).thenReturn(results())

        service.run(LocalDate.parse("2026-10-10"))
        service.run(LocalDate.parse("2026-10-11"))

        assertEquals(
            listOf(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-11")),
            service.list().map { it.reportDay },
        )
    }

    @Test
    fun `run accepts the clock calendar day`() {
        whenever(engine.calculate(any())).thenReturn(results())

        val receipt = service.run(LocalDate.parse("2026-10-12"))

        assertEquals(LocalDate.parse("2026-10-12"), receipt.reportDay)
        assertEquals(Instant.parse("2026-10-12T00:00:00Z"), receipt.windowFrom)
        assertEquals(Instant.parse("2026-10-12T23:59:59.999999999Z"), receipt.windowTo)
    }

    private fun results(): List<KpiResult> =
        listOf(
            KpiResult(
                id = KpiIds.EVENTS_CREATED_PER_DAY,
                value = KpiValue.Numeric(4.0),
                calculatedAt = calculatedAt,
            ),
        )

    private class RecordingExporter : KpiExporter {
        val exported = mutableListOf<KpiRun>()

        override val destination: String = "local"

        override val delivers: Boolean = false

        override fun export(run: KpiRun): KpiRunReceipt {
            exported += run
            return receipt(exported.size.toLong(), run)
        }

        override fun list(): List<KpiRunReceipt> =
            exported.mapIndexed { index, run ->
                receipt((index + 1).toLong(), run)
            }

        private fun receipt(
            id: Long,
            run: KpiRun,
        ): KpiRunReceipt =
            KpiRunReceipt(
                id = id,
                destination = destination,
                delivered = delivers,
                reportDay = run.reportDay,
                windowFrom = run.windowFrom,
                windowTo = run.windowTo,
                calculatedAt = run.results.first().calculatedAt,
                results = run.results,
            )
    }
}
