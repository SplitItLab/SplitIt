package edu.austral.splitit.server.application.port

import java.time.Instant

fun interface SharedEventCount {
    /** Events created in the window, inclusive, that have an invite link. */
    fun countSharedEventsCreated(
        from: Instant,
        to: Instant,
    ): Long
}
