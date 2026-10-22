package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunStatus
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseKpiExporterTest {
    private val base = "https://platform.test"
    private val runId = UUID.fromString("0192d36a-7b35-7cc4-b1bd-2d55f0d61a56")
    private val calculatedAt = Instant.parse("2026-10-12T03:15:00Z")
    private val catalogUrl = "$base/rest/v1/kpi_catalog"
    private val measurementUrl = "$base/rest/v1/measurement?on_conflict=project_id,kpi_id,date,env"

    private val fullCatalog =
        """[{"id":"events_created_per_day"},{"id":"expenses_registered_per_day"},
        {"id":"events_with_expense_percent"},{"id":"post_expenses_latency_p95"},
        {"id":"error_rate_5xx_24h"},{"id":"backend_health"},{"id":"last_successful_run"},
        {"id":"eventos_con_gasto_en_2_min_pct"},{"id":"eventos_compartidos_pct"},{"id":"latencia_p95_saldos"}]"""

    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()

    private fun exporter(projectKey: String = "project-secret") =
        SupabaseKpiExporter(
            builder.build(),
            SupabaseSettings(base, "publishable", projectKey, "dev"),
        ) { runId }

    private val run =
        KpiRun(
            reportDay = LocalDate.parse("2026-10-11"),
            windowFrom = Instant.parse("2026-10-11T00:00:00Z"),
            windowTo = Instant.parse("2026-10-11T23:59:59.999999Z"),
            results =
                listOf(
                    numeric(KpiIds.EVENTS_CREATED_PER_DAY, 4.0),
                    numeric(KpiIds.EXPENSES_REGISTERED_PER_DAY, 9.0),
                    numeric(KpiIds.EVENTS_WITH_EXPENSE_PERCENT, 50.0),
                    KpiResult(KpiIds.POST_EXPENSES_LATENCY_P95, null, calculatedAt),
                    numeric(KpiIds.ERROR_RATE_5XX_24H, 0.02),
                    KpiResult(KpiIds.BACKEND_HEALTH, KpiValue.Text("OK"), calculatedAt),
                    KpiResult(KpiIds.LAST_SUCCESSFUL_RUN, KpiValue.Text("2026-10-11T03:15:00Z"), calculatedAt),
                    numeric(KpiIds.EVENTS_WITH_EXPENSE_IN_2_MIN_PERCENT, 75.0),
                    numeric(KpiIds.SHARED_EVENTS_PERCENT, 0.0),
                    numeric(KpiIds.GET_BALANCES_LATENCY_P95, 42.5),
                ),
        )

    @Test
    fun `registers the catalog and then posts the batch with the platform headers`() {
        server
            .expect(requestTo("$catalogUrl?select=id"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("apikey", "publishable"))
            .andExpect(header("X-Project-Key", "project-secret"))
            .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo(catalogUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Project-Key", "project-secret"))
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].id").value("events_created_per_day"))
            .andExpect(jsonPath("$[0].kind").value("business"))
            .andExpect(jsonPath("$[3].unit").value("ms"))
            .andExpect(jsonPath("$[7].id").value("eventos_con_gasto_en_2_min_pct"))
            .andExpect(jsonPath("$[7].aggregation").value("avg"))
            .andExpect(jsonPath("$[8].id").value("eventos_compartidos_pct"))
            .andExpect(jsonPath("$[9].id").value("latencia_p95_saldos"))
            .andExpect(jsonPath("$[9].kind").value("technical"))
            .andRespond(withSuccess())
        server
            .expect(requestTo(measurementUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Prefer", "resolution=ignore-duplicates"))
            .andExpect(header("apikey", "publishable"))
            .andExpect(header("X-Project-Key", "project-secret"))
            .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.length()").value(9))
            .andExpect(jsonPath("$[0].kpi_id").value("events_created_per_day"))
            .andExpect(jsonPath("$[0].date").value("2026-10-11"))
            .andExpect(jsonPath("$[0].env").value("dev"))
            .andExpect(jsonPath("$[0].value").value(4.0))
            .andExpect(jsonPath("$[0].run_id").value(runId.toString()))
            .andExpect(jsonPath("$[0].project_id").doesNotExist())
            .andExpect(jsonPath("$[0].reported_at").doesNotExist())
            .andExpect(jsonPath("$[3].kpi_id").value("error_rate_5xx_24h"))
            .andExpect(jsonPath("$[4].kpi_id").value("backend_health"))
            .andExpect(jsonPath("$[4].value").value(1.0))
            .andExpect(jsonPath("$[5].kpi_id").value("last_successful_run"))
            .andExpect(jsonPath("$[5].value").value(1_791_688_500.0))
            .andExpect(jsonPath("$[6].kpi_id").value("eventos_con_gasto_en_2_min_pct"))
            .andExpect(jsonPath("$[6].value").value(75.0))
            .andExpect(jsonPath("$[7].kpi_id").value("eventos_compartidos_pct"))
            .andExpect(jsonPath("$[7].value").value(0.0))
            .andExpect(jsonPath("$[8].kpi_id").value("latencia_p95_saldos"))
            .andExpect(jsonPath("$[8].value").value(42.5))
            .andRespond(withSuccess())

        val receipt = exporter().export(run)

        server.verify()
        assertEquals("supabase", receipt.destination)
        assertEquals(KpiRunStatus.SENT, receipt.status)
        assertEquals(run.reportDay, receipt.reportDay)
    }

    @Test
    fun `only registers the catalog entries the platform does not have yet`() {
        server
            .expect(requestTo("$catalogUrl?select=id"))
            .andRespond(
                withSuccess(fullCatalog, MediaType.APPLICATION_JSON),
            )
        server
            .expect(requestTo(measurementUrl))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess())

        val receipt = exporter().export(run)

        server.verify()
        assertEquals(KpiRunStatus.SENT, receipt.status)
    }

    @Test
    fun `sends the batch when another instance registered the catalog first`() {
        server.expect(requestTo("$catalogUrl?select=id")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo(catalogUrl))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CONFLICT))
        server
            .expect(requestTo("$catalogUrl?select=id"))
            .andRespond(withSuccess(fullCatalog, MediaType.APPLICATION_JSON))
        server.expect(requestTo(measurementUrl)).andRespond(withSuccess())

        val receipt = exporter().export(run)

        server.verify()
        assertEquals(KpiRunStatus.SENT, receipt.status)
    }

    @Test
    fun `does not send the batch when the catalog is still incomplete after a conflict`() {
        server.expect(requestTo("$catalogUrl?select=id")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo(catalogUrl))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CONFLICT))
        server
            .expect(requestTo("$catalogUrl?select=id"))
            .andRespond(withSuccess("""[{"id":"events_created_per_day"}]""", MediaType.APPLICATION_JSON))
        server.expect(never(), requestTo(measurementUrl)).andRespond(withSuccess())

        val receipt = exporter().export(run)

        server.verify()
        assertEquals("supabase", receipt.destination)
        assertEquals(KpiRunStatus.FAILED, receipt.status)
    }

    @Test
    fun `skips the send with a warning and no request when the project key is missing`() {
        server.expect(never(), requestTo(measurementUrl)).andRespond(withSuccess())

        val receipt = exporter(projectKey = "").export(run)

        server.verify()
        assertEquals(KpiRunStatus.SKIPPED, receipt.status)
        assertEquals(run.results, receipt.results)
    }

    @Test
    fun `a platform failure is logged and does not break the run`() {
        server.expect(requestTo("$catalogUrl?select=id")).andRespond(withServerError())

        val receipt = exporter().export(run)

        server.verify()
        assertEquals("supabase", receipt.destination)
        assertEquals(KpiRunStatus.FAILED, receipt.status)
    }

    @Test
    fun `does not send when no value is numeric`() {
        server.expect(never(), requestTo(measurementUrl)).andRespond(withSuccess())
        val empty = run.copy(results = listOf(KpiResult(KpiIds.POST_EXPENSES_LATENCY_P95, null, calculatedAt)))

        val receipt = exporter().export(empty)

        server.verify()
        assertEquals(KpiRunStatus.SKIPPED, receipt.status)
    }

    @Test
    fun `catalog ids are snake case and fit the platform limits`() {
        val pattern = Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
        val ids = SupabaseKpiCatalog.entries.map { it.platformId }

        // The platform keeps at most 10 active KPIs per env.
        assertEquals(10, ids.toSet().size)
        assertTrue(ids.all { pattern.matches(it) && it.length <= 64 })
        assertTrue(SupabaseKpiCatalog.entries.all { it.kind in setOf("business", "technical", "health") })
        assertTrue(SupabaseKpiCatalog.entries.all { it.description.length in 5..500 })
    }

    private fun numeric(
        id: String,
        amount: Double,
    ) = KpiResult(id, KpiValue.Numeric(amount), calculatedAt)
}
