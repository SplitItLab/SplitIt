package edu.austral.splitit.server.application.port

import java.time.Instant

fun interface ExpenseRegistrationCount {
    fun countExpensesRegistered(
        from: Instant,
        to: Instant,
    ): Long
}
