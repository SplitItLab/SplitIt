package edu.austral.splitit.server.application.port

import java.time.Instant

fun interface EventCreationCount {
    fun countEventsCreated(
        from: Instant,
        to: Instant,
    ): Long
}
