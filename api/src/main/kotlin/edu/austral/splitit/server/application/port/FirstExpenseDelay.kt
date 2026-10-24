package edu.austral.splitit.server.application.port

import java.time.Duration
import java.time.Instant

fun interface FirstExpenseDelay {
    /** One entry per event created in the window, inclusive: time until its first expense, or null without expenses. */
    fun firstExpenseDelays(
        from: Instant,
        to: Instant,
    ): List<Duration?>
}
