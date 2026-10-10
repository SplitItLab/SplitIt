package edu.austral.splitit.server.application.port

import java.time.Instant

interface LastRunPort {
    fun recordSuccessfulRun(at: Instant)

    fun lastSuccessfulRun(): Instant?
}
