package edu.austral.splitit.server.infrastructure.api.kpi

import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.application.service.RunKpiReport
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate

@WebMvcTest(
    controllers = [KpiRunController::class],
    excludeAutoConfiguration = [
        UserDetailsServiceAutoConfiguration::class,
    ],
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "jwt.expiration-hours=8",
        "jwt.secret=test-jwt-secret-that-is-long-enough",
    ],
)
class KpiRunControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var runKpiReport: RunKpiReport

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `an authenticated post does not run a report`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)

        mockMvc
            .perform(
                post("/api/kpi-runs")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reportDay":"2026-10-10"}"""),
            ).andExpect(status().isMethodNotAllowed)

        verify(runKpiReport, never()).run(any())
    }

    @Test
    fun `post without a session returns 401`() {
        mockMvc
            .perform(post("/api/kpi-runs"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(runKpiReport, never()).run(any())
    }

    @Test
    fun `get returns the stored runs for the active exporter`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(runKpiReport.list()).thenReturn(
            listOf(
                receipt(LocalDate.parse("2026-10-10"), id = 1),
                receipt(LocalDate.parse("2026-10-11"), id = 2),
            ),
        )

        mockMvc
            .perform(get("/api/kpi-runs").cookie(Cookie("auth_token", "good-token")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].reportDay").value("2026-10-10"))
            .andExpect(jsonPath("$[1].reportDay").value("2026-10-11"))
            .andExpect(jsonPath("$[0].destination").value("local"))
            .andExpect(jsonPath("$[1].destination").value("local"))
    }

    @Test
    fun `get without a session returns 401`() {
        mockMvc
            .perform(get("/api/kpi-runs"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(runKpiReport, never()).list()
    }

    @Test
    fun `get is forbidden for a guest`() {
        whenever(tokenProvider.parseGuest("guest-token"))
            .thenReturn(AuthGuest(eventId = 10L, memberId = 30L, name = "Juan"))

        mockMvc
            .perform(get("/api/kpi-runs").cookie(Cookie("guest_token", "guest-token")))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))

        verify(runKpiReport, never()).list()
    }

    private fun receipt(
        day: LocalDate,
        id: Long = 8,
    ): KpiRunReceipt =
        KpiRunReceipt(
            id = id,
            destination = "local",
            reportDay = day,
            windowFrom = Instant.parse("${day}T00:00:00Z"),
            windowTo = Instant.parse("${day}T23:59:59.999999999Z"),
            calculatedAt = Instant.parse("2026-10-12T03:15:00Z"),
            results =
                listOf(
                    KpiResult(KpiIds.EVENTS_CREATED_PER_DAY, KpiValue.Numeric(NUMERIC), CALCULATED_AT),
                    KpiResult(KpiIds.BACKEND_HEALTH, KpiValue.Text("OK"), CALCULATED_AT),
                    KpiResult(KpiIds.ERROR_RATE_5XX_24H, null, CALCULATED_AT),
                ),
        )

    private companion object {
        const val NUMERIC = 4.0
        val CALCULATED_AT: Instant = Instant.parse("2026-10-12T03:15:00Z")
        val authUser =
            AuthUser(
                id = 1L,
                username = "mateo@example.com",
                password = "",
                roles = emptyList(),
                name = "Mateo",
            )
    }
}
