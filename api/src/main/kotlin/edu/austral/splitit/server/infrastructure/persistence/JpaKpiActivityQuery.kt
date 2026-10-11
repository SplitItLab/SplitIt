package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.application.port.EventExpenseCoverage
import edu.austral.splitit.server.application.port.ExpenseRegistrationCount
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class JpaKpiActivityQuery(
    private val eventRepository: EventRepository,
    private val expenseRepository: ExpenseRepository,
) : EventCreationCount,
    ExpenseRegistrationCount,
    EventExpenseCoverage {
    override fun countEventsCreated(
        from: Instant,
        to: Instant,
    ): Long = eventRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(from, to)

    override fun countExpensesRegistered(
        from: Instant,
        to: Instant,
    ): Long = expenseRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(from, to)

    override fun countEventsWithAtLeastOneExpense(): Long = eventRepository.countWithAtLeastOneExpense()

    override fun countEvents(): Long = eventRepository.count()
}
