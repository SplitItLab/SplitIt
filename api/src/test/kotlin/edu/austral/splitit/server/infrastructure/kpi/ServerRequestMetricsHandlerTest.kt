package edu.austral.splitit.server.infrastructure.kpi

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import edu.austral.splitit.server.infrastructure.persistence.JpaRequestMetrics
import edu.austral.splitit.server.infrastructure.persistence.RequestSample
import io.micrometer.observation.Observation
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerRequestMetricsHandlerTest {
    private val at = Instant.parse("2026-10-10T15:00:00Z")
    private val clock = Clock.fixed(at, ZoneOffset.UTC)
    private val metrics = mock<JpaRequestMetrics>()
    private val handler = ServerRequestMetricsHandler(metrics, clock)

    @Test
    fun `a completed request records status duration and route pattern`() {
        val context = context("GET", "/api/status", 200, "/api/status")

        handler.onStart(context)
        handler.onStop(context)

        val sample = recorded()
        assertEquals("GET /api/status", sample.endpoint)
        assertEquals(200, sample.statusCode)
        assertTrue(sample.duration >= Duration.ZERO)
        assertEquals(at, sample.at)
    }

    @Test
    fun `a rejection without a route pattern records the request URI`() {
        val context = context("GET", "/api/events", 401, null)

        handler.onStart(context)
        handler.onStop(context)

        val sample = recorded()
        assertEquals("GET /api/events", sample.endpoint)
        assertEquals(401, sample.statusCode)
    }

    @Test
    fun `supports only server request observations`() {
        assertTrue(handler.supportsContext(context("GET", "/api/status", 200, "/api/status")))
        assertTrue(!handler.supportsContext(Observation.Context()))
    }

    @Test
    fun `a failed insert does not log the client request URI`() {
        doThrow(DataAccessResourceFailureException("database down")).whenever(metrics).record(any())
        val context = context("GET", "/api/invite/secret-token", 404, null)

        handler.onStart(context)
        val messages = captureLogs { handler.onStop(context) }

        assertEquals(1, messages.size)
        assertFalse(messages.single().contains("secret-token"))
    }

    private fun captureLogs(block: () -> Unit): List<String> {
        val logger = LoggerFactory.getLogger(ServerRequestMetricsHandler::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        try {
            block()
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
        return appender.list.map { it.formattedMessage }
    }

    private fun recorded(): RequestSample {
        val sample = argumentCaptor<RequestSample>()
        verify(metrics).record(sample.capture())
        return sample.firstValue
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
