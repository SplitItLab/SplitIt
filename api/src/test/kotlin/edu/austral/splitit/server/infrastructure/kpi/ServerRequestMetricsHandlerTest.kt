package edu.austral.splitit.server.infrastructure.kpi

import io.micrometer.observation.Observation
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerRequestMetricsHandlerTest {
    private val at = Instant.parse("2026-10-10T15:00:00Z")
    private val clock = Clock.fixed(at, ZoneOffset.UTC)
    private val metrics = InMemoryRequestMetrics(clock)
    private val handler = ServerRequestMetricsHandler(metrics, clock)
    private val window = at.minusSeconds(1)..at.plusSeconds(1)

    @Test
    fun `a completed request records status duration and route pattern`() {
        val context = context("GET", "/api/status", 200, "/api/status")

        handler.onStart(context)
        handler.onStop(context)

        val duration = metrics.p95("GET /api/status", window)
        assertTrue(duration != null && duration >= Duration.ZERO)
        assertEquals(0.0, metrics.rate5xx(window))
    }

    @Test
    fun `a rejection without a route pattern records the request URI and is not a 5xx`() {
        val context = context("GET", "/api/events", 401, null)

        handler.onStart(context)
        handler.onStop(context)

        assertTrue(metrics.p95("GET /api/events", window) != null)
        assertEquals(0.0, metrics.rate5xx(window))
        assertNull(metrics.p95("POST /api/events/{eventId}/expenses", window))
    }

    @Test
    fun `supports only server request observations`() {
        assertTrue(handler.supportsContext(context("GET", "/api/status", 200, "/api/status")))
        assertTrue(!handler.supportsContext(Observation.Context()))
    }

    private fun context(
        method: String,
        uri: String,
        status: Int,
        pattern: String?,
    ): ServerRequestObservationContext {
        val request = MockHttpServletRequest(method, uri)
        val response = MockHttpServletResponse()
        response.status = status
        val observation = ServerRequestObservationContext(request, response)
        pattern?.let { observation.setPathPattern(it) }
        observation.setName("http.server.requests")
        return observation
    }
}
