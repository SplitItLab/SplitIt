package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class KpiSnapshotExporterTest {
    private val store = mock<JpaKpiSnapshotStore>()
    private val run =
        KpiRun(
            reportDay = LocalDate.parse("2026-10-11"),
            windowFrom = Instant.parse("2026-10-11T00:00:00Z"),
            windowTo = Instant.parse("2026-10-11T23:59:59.999999999Z"),
            results =
                listOf(
                    KpiResult(
                        id = KpiIds.EVENTS_CREATED_PER_DAY,
                        value = KpiValue.Numeric(4.0),
                        calculatedAt = Instant.parse("2026-10-12T03:15:00Z"),
                    ),
                ),
        )

    @Test
    fun `export and list stay on the channel the exporter was given`() {
        whenever(store.append(any(), any())).thenAnswer { invocation ->
            stored(invocation.getArgument(0))
        }
        whenever(store.list("report")).thenReturn(listOf(stored("report")))
        val exporter = KpiSnapshotExporter(store, "report")

        val receipt = exporter.export(run)
        val listed = exporter.list()

        assertEquals("report", receipt.destination)
        assertEquals("report", listed.single().destination)
        verify(store).append("report", run)
        verify(store).list("report")
        verify(store, never()).append(org.mockito.kotlin.eq("local"), any())
        verify(store, never()).list("local")
    }

    private fun stored(channel: String): KpiRunReceipt =
        KpiRunReceipt(
            id = 1,
            destination = channel,
            reportDay = run.reportDay,
            windowFrom = run.windowFrom,
            windowTo = run.windowTo,
            calculatedAt = run.results.first().calculatedAt,
            results = run.results,
        )
}
