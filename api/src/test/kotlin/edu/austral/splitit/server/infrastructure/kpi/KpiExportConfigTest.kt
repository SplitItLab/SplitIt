package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.application.service.KpiRunStatus
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KpiExportConfigTest {
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
    fun `local mode writes the local channel`() {
        assertChannel("kpi.export.mode=local", "local")
    }

    @Test
    fun `missing mode writes the local channel`() {
        assertChannel(null, "local")
    }

    @Test
    fun `report mode writes the report channel`() {
        assertChannel("kpi.export.mode=report", "report")
    }

    @Test
    fun `an unknown mode fails startup`() {
        val store = mock<JpaKpiSnapshotStore>()
        contextRunner(store, "kpi.export.mode=kafka").run { context ->
            val failure = assertNotNull(context.startupFailure)
            assertTrue(failure.stackTraceToString().contains(MODE_ERROR))
        }
    }

    @Test
    fun `supabase mode sends to the platform instead of the local store`() {
        val store = mock<JpaKpiSnapshotStore>()
        contextRunner(store, "kpi.export.mode=supabase").run { context ->
            assertNull(context.startupFailure)
            assertTrue(context.getBean(KpiExporter::class.java) is SupabaseKpiExporter)
            verify(store, never()).append(any(), any())
        }
    }

    @Test
    fun `an unknown export env fails startup in supabase mode`() {
        val store = mock<JpaKpiSnapshotStore>()
        contextRunner(store, "kpi.export.mode=supabase", "kpi.export.env=staging").run { context ->
            val failure = assertNotNull(context.startupFailure)
            assertTrue(failure.stackTraceToString().contains("kpi.export.env must be dev, qa or prod"))
        }
    }

    private fun assertChannel(
        property: String?,
        channel: String,
    ) {
        val store = mock<JpaKpiSnapshotStore>()
        whenever(store.append(any(), any())).thenAnswer { invocation ->
            stored(invocation.getArgument(0))
        }
        contextRunner(store, property).run { context ->
            assertNull(context.startupFailure)
            val exporters = context.getBeansOfType(KpiExporter::class.java)
            assertEquals(1, exporters.size)
            exporters.values.single().export(run)
            verify(store).append(channel, run)
        }
    }

    private fun contextRunner(
        store: JpaKpiSnapshotStore,
        vararg properties: String?,
    ): ApplicationContextRunner =
        ApplicationContextRunner()
            .withBean(JpaKpiSnapshotStore::class.java, { store })
            .withBean(RestClient::class.java, { RestClient.create() })
            .withUserConfiguration(KpiExportConfig::class.java)
            .withPropertyValues(*properties.filterNotNull().toTypedArray())

    private fun stored(channel: String): KpiRunReceipt =
        KpiRunReceipt(
            id = 1,
            destination = channel,
            status = KpiRunStatus.SENT,
            reportDay = run.reportDay,
            windowFrom = run.windowFrom,
            windowTo = run.windowTo,
            calculatedAt = run.results.first().calculatedAt,
            results = run.results,
        )

    private companion object {
        const val MODE_ERROR = "kpi.export.mode must be local, report or supabase"
    }
}
