package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.application.service.ExpenseApplicationService
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.infrastructure.api.StatusController
import edu.austral.splitit.server.infrastructure.api.event.EventController
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import edu.austral.splitit.server.infrastructure.persistence.JpaRequestMetrics
import edu.austral.splitit.server.infrastructure.persistence.RequestSample
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@WebMvcTest(
    controllers = [StatusController::class, EventController::class],
    excludeAutoConfiguration = [UserDetailsServiceAutoConfiguration::class],
)
@AutoConfigureMockMvc
@Import(
    SecurityConfig::class,
    ServerRequestMetricsHandler::class,
    ServerRequestObservationTest.ClockConfig::class,
    ObservationAutoConfiguration::class,
    MetricsAutoConfiguration::class,
    WebMvcObservationAutoConfiguration::class,
)
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "jwt.expiration-hours=8",
        "jwt.secret=test-jwt-secret-that-is-long-enough",
    ],
)
class ServerRequestObservationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var metrics: JpaRequestMetrics

    @MockitoBean
    private lateinit var eventApplicationService: EventApplicationService

    @MockitoBean
    private lateinit var expenseApplicationService: ExpenseApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    @Test
    fun `a real status request records status duration and endpoint`() {
        mockMvc.perform(get("/api/status")).andExpect(status().isOk())

        val sample = recorded()
        assertEquals("GET /api/status", sample.endpoint)
        assertEquals(200, sample.statusCode)
        assertTrue(sample.duration >= Duration.ZERO)
    }

    @Test
    fun `a real unauthorized request is recorded with its status`() {
        mockMvc.perform(get("/api/events")).andExpect(status().isUnauthorized())

        val sample = recorded()
        assertEquals("GET /api/events", sample.endpoint)
        assertEquals(401, sample.statusCode)
    }

    @Test
    fun `a failed sample insert does not fail the request`() {
        doThrow(DataAccessResourceFailureException("database down")).whenever(metrics).record(any())

        mockMvc.perform(get("/api/status")).andExpect(status().isOk())

        // Wait for the async insert attempt, or it lands in the next test after the mock is reset.
        verify(metrics, timeout(TIMEOUT_MILLIS)).record(any())
    }

    private fun recorded(): RequestSample {
        val sample = argumentCaptor<RequestSample>()
        verify(metrics, timeout(TIMEOUT_MILLIS)).record(sample.capture())
        return sample.firstValue
    }

    private companion object {
        const val TIMEOUT_MILLIS = 1_000L
    }

    @TestConfiguration
    class ClockConfig {
        @Bean
        fun clock(): Clock = Clock.systemUTC()
    }
}
