package edu.austral.splitit.server.infrastructure.kpi

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/** Copies each finished incoming HTTP observation into the windowed KPI store. */
@Component
class ServerRequestMetricsHandler(
    private val metrics: InMemoryRequestMetrics,
    private val clock: Clock,
) : ObservationHandler<ServerRequestObservationContext> {
    override fun supportsContext(context: Observation.Context): Boolean = context is ServerRequestObservationContext

    override fun onStart(context: ServerRequestObservationContext) {
        context.put(START_NANOS, System.nanoTime())
    }

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
        metrics.record(
            RequestSample(
                endpoint = "${request.method} $route",
                statusCode = response.status,
                duration = Duration.ofNanos(elapsed),
                at = clock.instant(),
            ),
        )
    }

    private companion object {
        const val START_NANOS = "splitit.request.startNanos"
        const val MIN_STATUS = 100
    }
}
