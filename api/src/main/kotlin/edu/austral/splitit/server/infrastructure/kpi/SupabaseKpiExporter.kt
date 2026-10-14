package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.util.UUID

data class SupabaseSettings(
    val url: String,
    val publishableKey: String,
    val projectKey: String,
    val env: String,
) {
    val canSend: Boolean get() = url.isNotBlank() && publishableKey.isNotBlank() && projectKey.isNotBlank()
}

/**
 * Sends each run to the central Supabase platform: registers the missing catalog entries,
 * then posts the daily batch. The platform keeps the first value per (kpi, date, env),
 * so resending a run does not duplicate it.
 */
class SupabaseKpiExporter(
    private val restClient: RestClient,
    private val settings: SupabaseSettings,
    private val runIdSupplier: () -> UUID = UUID::randomUUID,
) : KpiExporter {
    override fun export(run: KpiRun): KpiRunReceipt {
        if (!settings.canSend) {
            logger.warn(
                "KPI export skipped: no {} key configured. reportDay={}",
                settings.env,
                run.reportDay,
            )
        } else {
            send(run)
        }
        return run.toReceipt()
    }

    override fun list(): List<KpiRunReceipt> = emptyList()

    private fun send(run: KpiRun) {
        val batch = batchFor(run)
        if (batch.isEmpty()) {
            logger.warn("KPI export skipped: no numeric values to send. reportDay={}", run.reportDay)
            return
        }
        try {
            registerMissingCatalogEntries()
            post(MEASUREMENT_PATH, batch, ignoreDuplicates = true)
            logger.info("KPI export sent. env={} reportDay={} rows={}", settings.env, run.reportDay, batch.size)
        } catch (exception: RestClientException) {
            logger.error(
                "KPI export failed. env={} reportDay={} reason={}",
                settings.env,
                run.reportDay,
                exception.message,
            )
        }
    }

    private fun batchFor(run: KpiRun): List<Map<String, Any>> {
        val runId = runIdSupplier().toString()
        return run.results.mapNotNull { result ->
            val kpi = SupabaseKpiCatalog.find(result.id)
            val measurement = kpi?.let { SupabaseKpiCatalog.measurement(it, result.value) }
            if (kpi == null || measurement == null) {
                logger.info("KPI value not exported. id={}", result.id)
                null
            } else {
                mapOf(
                    "kpi_id" to kpi.platformId,
                    "date" to run.reportDay.toString(),
                    "env" to settings.env,
                    "value" to measurement,
                    "run_id" to runId,
                )
            }
        }
    }

    private fun registerMissingCatalogEntries() {
        val missing = missingCatalogEntries()
        if (missing.isEmpty()) return
        try {
            post(CATALOG_PATH, missing.map { it.catalogBody() }, ignoreDuplicates = false)
            logger.info("KPI catalog registered. env={} kpis={}", settings.env, missing.map { it.platformId })
        } catch (conflict: HttpClientErrorException.Conflict) {
            // Another instance registered them between the read and the post.
            val stillMissing = missingCatalogEntries()
            if (stillMissing.isNotEmpty()) throw conflict
        }
    }

    private fun missingCatalogEntries(): List<SupabaseKpi> {
        val registered =
            restClient
                .get()
                .uri(settings.url + CATALOG_LIST_PATH)
                .headers(::addPlatformKeys)
                .retrieve()
                .body<List<CatalogRow>>()
                .orEmpty()
                .map { it.id }
                .toSet()
        return SupabaseKpiCatalog.entries.filter { it.platformId !in registered }
    }

    private fun post(
        path: String,
        body: Any,
        ignoreDuplicates: Boolean,
    ) {
        restClient
            .post()
            .uri(settings.url + path)
            .headers { headers ->
                addPlatformKeys(headers)
                if (ignoreDuplicates) headers.set("Prefer", IGNORE_DUPLICATES)
            }.contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity()
    }

    // The project key goes in X-Project-Key, never in Authorization (PostgREST would read it as a JWT).
    private fun addPlatformKeys(headers: HttpHeaders) {
        headers.set("apikey", settings.publishableKey)
        headers.set("X-Project-Key", settings.projectKey)
    }

    private fun KpiRun.toReceipt(): KpiRunReceipt =
        KpiRunReceipt(
            id = 0,
            destination = DESTINATION,
            reportDay = reportDay,
            windowFrom = windowFrom,
            windowTo = windowTo,
            calculatedAt = results.firstOrNull()?.calculatedAt ?: windowTo,
            results = results,
        )

    private data class CatalogRow(
        val id: String,
    )

    private companion object {
        const val DESTINATION = "supabase"
        const val IGNORE_DUPLICATES = "resolution=ignore-duplicates"
        const val MEASUREMENT_PATH = "/rest/v1/measurement?on_conflict=project_id,kpi_id,date,env"
        const val CATALOG_PATH = "/rest/v1/kpi_catalog"
        const val CATALOG_LIST_PATH = "/rest/v1/kpi_catalog?select=id"
        val logger: Logger = LoggerFactory.getLogger(SupabaseKpiExporter::class.java)
    }
}
