package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.ErrorRatePort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class InMemoryErrorRate
    @JvmOverloads
    constructor(
        private val samples: List<HttpStatusSample> = emptyList(),
    ) : ErrorRatePort {
        override fun rate5xx(window: ClosedRange<Instant>): Double? {
            val inWindow = samples.filter { it.at in window }
            if (inWindow.isEmpty()) {
                return null
            }
            val serverErrors = inWindow.count { it.statusCode in SERVER_ERROR }
            return serverErrors.toDouble() / inWindow.size.toDouble()
        }

        private companion object {
            const val SERVER_ERROR_MIN = 500
            const val SERVER_ERROR_MAX = 599
            val SERVER_ERROR: IntRange = SERVER_ERROR_MIN..SERVER_ERROR_MAX
        }
    }
