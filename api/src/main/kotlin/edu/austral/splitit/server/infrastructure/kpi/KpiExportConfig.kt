package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class KpiExportConfig {
    @Bean
    fun kpiExporter(
        store: JpaKpiSnapshotStore,
        environment: Environment,
    ): KpiExporter {
        val mode = environment.getProperty(MODE, LOCAL)
        require(mode in CHANNELS) { "kpi.export.mode must be local or report" }
        return KpiSnapshotExporter(store, mode)
    }

    private companion object {
        const val MODE = "kpi.export.mode"
        const val LOCAL = "local"
        val CHANNELS = setOf(LOCAL, "report")
    }
}
