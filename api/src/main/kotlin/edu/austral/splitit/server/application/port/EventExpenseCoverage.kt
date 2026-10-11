package edu.austral.splitit.server.application.port

interface EventExpenseCoverage {
    fun countEventsWithAtLeastOneExpense(): Long

    fun countEvents(): Long
}
