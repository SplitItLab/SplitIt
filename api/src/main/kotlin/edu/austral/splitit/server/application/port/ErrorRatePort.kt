package edu.austral.splitit.server.application.port

import java.time.Instant

fun interface ErrorRatePort {
    /** 4xx is not an error. Null means there are no samples in the window. */
    fun rate5xx(window: ClosedRange<Instant>): Double?
}
