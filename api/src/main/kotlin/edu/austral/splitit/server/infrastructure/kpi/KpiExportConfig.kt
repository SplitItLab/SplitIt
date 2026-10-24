package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.client.RestClient

@Configuration
class KpiExportConfig {
    @Bean
    fun kpiExporter(
        store: JpaKpiSnapshotStore,
        environment: Environment,
        restClient: ObjectProvider<RestClient>,
    ): KpiExporter {
        val mode = environment.getProperty(MODE, LOCAL)
        require(mode in MODES) { "kpi.export.mode must be local, report or supabase" }
        if (mode == SUPABASE) {
            return SupabaseKpiExporter(restClient.getObject(), supabaseSettings(environment))
        }
        return KpiSnapshotExporter(store, mode)
    }

    private fun supabaseSettings(environment: Environment): SupabaseSettings {
        val env = environment.getProperty(ENV, DEFAULT_ENV)
        require(env in ENVIRONMENTS) { "kpi.export.env must be dev, qa or prod" }
        return SupabaseSettings(
            url = environment.getProperty("kpi.export.supabase.url", "").trimEnd('/'),
            publishableKey = environment.getProperty("kpi.export.supabase.publishable-key", ""),
            projectKey = environment.getProperty("kpi.export.supabase.project-key", ""),
            env = env,
        )
    }

    private companion object {
        const val MODE = "kpi.export.mode"
        const val ENV = "kpi.export.env"
        const val LOCAL = "local"
        const val SUPABASE = "supabase"
        const val DEFAULT_ENV = "dev"
        val MODES = setOf(LOCAL, "report", SUPABASE)
        val ENVIRONMENTS = setOf("dev", "qa", "prod")
    }
}
