package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KpiExportConfigTest {
    private val store = mock<JpaKpiSnapshotStore>()

    @Test
    fun `local mode exposes only the local exporter`() {
        assertExporter("kpi.export.mode=local", LocalKpiExporter::class.java)
    }

    @Test
    fun `missing mode exposes only the local exporter`() {
        assertExporter(null, LocalKpiExporter::class.java)
    }

    @Test
    fun `report mode exposes only the report exporter`() {
        assertExporter("kpi.export.mode=report", ReportKpiExporter::class.java)
    }

    private fun assertExporter(
        property: String?,
        expected: Class<out KpiExporter>,
    ) {
        var runner =
            ApplicationContextRunner()
                .withBean(JpaKpiSnapshotStore::class.java, { store })
                .withUserConfiguration(KpiExportConfig::class.java)
        if (property != null) {
            runner = runner.withPropertyValues(property)
        }
        runner.run { context ->
            assertNull(context.startupFailure)
            val exporters = context.getBeansOfType(KpiExporter::class.java)
            assertEquals(1, exporters.size)
            assertTrue(expected.isInstance(exporters.values.single()))
        }
    }
}
