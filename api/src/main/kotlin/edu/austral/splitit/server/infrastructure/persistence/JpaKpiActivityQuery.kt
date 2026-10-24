package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.application.port.EventExpenseCoverage
import edu.austral.splitit.server.application.port.ExpenseRegistrationCount
import edu.austral.splitit.server.application.port.FirstExpenseDelay
import edu.austral.splitit.server.application.port.SharedEventCount
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant

@Repository
class JpaKpiActivityQuery(
    private val eventRepository: EventRepository,
    private val expenseRepository: ExpenseRepository,
) : EventCreationCount,
    ExpenseRegistrationCount,
    EventExpenseCoverage,
    FirstExpenseDelay,
    SharedEventCount {
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

    // The delay is computed here: date arithmetic in JPQL differs between H2 and Postgres.
    override fun firstExpenseDelays(
        from: Instant,
        to: Instant,
    ): List<Duration?> =
        eventRepository.findFirstExpenseOfEventsCreatedBetween(from, to).map { event ->
            event.firstExpenseAt?.let { Duration.between(event.createdAt, it) }
        }

    override fun countSharedEventsCreated(
        from: Instant,
        to: Instant,
    ): Long = eventRepository.countCreatedBetweenWithInviteLink(from, to)
}
