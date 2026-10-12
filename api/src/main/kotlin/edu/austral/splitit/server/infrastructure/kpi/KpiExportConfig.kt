package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KpiExportConfig {
    @Bean
    @ConditionalOnProperty(name = ["kpi.export.mode"], havingValue = "report")
    fun reportKpiExporter(store: JpaKpiSnapshotStore): KpiExporter = ReportKpiExporter(store)

    @Bean
    @ConditionalOnProperty(
        name = ["kpi.export.mode"],
        havingValue = "local",
        matchIfMissing = true,
    )
    fun localKpiExporter(store: JpaKpiSnapshotStore): KpiExporter = LocalKpiExporter(store)
}
