package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.infrastructure.persistence.JpaRequestMetrics
import edu.austral.splitit.server.infrastructure.persistence.RequestSample
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/** Stores each finished incoming HTTP observation as a request sample for the KPI ports. */
@Component
class ServerRequestMetricsHandler(
    private val metrics: JpaRequestMetrics,
    private val clock: Clock,
) : ObservationHandler<ServerRequestObservationContext> {
    override fun supportsContext(context: Observation.Context): Boolean = context is ServerRequestObservationContext

    override fun onStart(context: ServerRequestObservationContext) {
        context.put(START_NANOS, System.nanoTime())
    }

    @Suppress("TooGenericExceptionCaught") // una muestra perdida no puede romper la request (SPT-87)
    override fun onStop(context: ServerRequestObservationContext) {
        val started = context.get<Long>(START_NANOS)
        val response = context.response
        val request = context.carrier
        if (started == null || response == null || request == null) {
            return
        }
        if (response.status < MIN_STATUS) {
            return
        }
        val route = context.pathPattern ?: request.requestURI
        val elapsed = (System.nanoTime() - started).coerceAtLeast(0)
        val sample =
            RequestSample(
                endpoint = "${request.method} $route",
                statusCode = response.status,
                duration = Duration.ofNanos(elapsed),
                at = clock.instant(),
            )
        try {
            metrics.record(sample)
        } catch (exception: Exception) {
            // Without a route pattern the endpoint is the client URI, and the message may echo it.
            logger.warn("Request sample not stored. reason={}", exception.javaClass.name)
        }
    }

    private companion object {
        const val START_NANOS = "splitit.request.startNanos"
        const val MIN_STATUS = 100
        val logger: Logger = LoggerFactory.getLogger(ServerRequestMetricsHandler::class.java)
    }
}
