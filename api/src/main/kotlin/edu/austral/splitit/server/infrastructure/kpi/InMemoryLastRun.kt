package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.LastRunPort
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
class InMemoryLastRun : LastRunPort {
    private val storedAt = AtomicReference<Instant?>(null)

    override fun recordSuccessfulRun(at: Instant) {
        storedAt.set(at)
    }

    override fun lastSuccessfulRun(): Instant? = storedAt.get()
}
