package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.infrastructure.api.StatusController
import edu.austral.splitit.server.infrastructure.api.event.EventController
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import org.junit.jupiter.api.Test
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebMvcTest(
    controllers = [StatusController::class, EventController::class],
    excludeAutoConfiguration = [UserDetailsServiceAutoConfiguration::class],
)
@AutoConfigureMockMvc
@Import(
    SecurityConfig::class,
    InMemoryRequestMetrics::class,
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
    @Autowired private val metrics: InMemoryRequestMetrics,
) {
    @MockitoBean
    private lateinit var eventApplicationService: EventApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `a real status request records status duration and endpoint`() {
        mockMvc.perform(get("/api/status")).andExpect(status().isOk())

        val duration = metrics.p95("GET /api/status", window())
        assertNotNull(duration)
        assertTrue(duration >= Duration.ZERO)
        assertEquals(0.0, metrics.rate5xx(window()))
    }

    @Test
    fun `a real unauthorized request is recorded and is not a 5xx`() {
        mockMvc.perform(get("/api/events")).andExpect(status().isUnauthorized())

        assertNotNull(metrics.p95("GET /api/events", window()))
        assertEquals(0.0, metrics.rate5xx(window()))
    }

    private fun window(): ClosedRange<Instant> {
        val now = Instant.now()
        return now.minusSeconds(60)..now.plusSeconds(5)
    }

    @TestConfiguration
    class ClockConfig {
        @Bean
        fun clock(): Clock = Clock.systemUTC()
    }
}
