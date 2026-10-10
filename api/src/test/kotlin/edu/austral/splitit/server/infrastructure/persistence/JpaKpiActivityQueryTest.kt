package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.model.user.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaKpiActivityQuery::class)
@TestPropertySource(
    properties = [
        "POSTGRES_HOST=localhost",
        "POSTGRES_USER=test",
        "POSTGRES_PASSWORD=test",
        "JWT_SECRET=test-jwt-secret-that-is-long-enough",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
    ],
)
class JpaKpiActivityQueryTest(
    @Autowired private val query: JpaKpiActivityQuery,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
    @Autowired private val expenseRepository: ExpenseRepository,
) {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")

    @BeforeEach
    fun seed() {
        val owner =
            userRepository.saveAndFlush(
                Helpers.user(name = "Ada Lovelace", email = "ada-kpi@example.com", id = null),
            )
        persistEvent(owner, "Viaje sin gasto", from)
        val insideWithExpense = persistEvent(owner, "Viaje con gasto", to)
        persistExpense(insideWithExpense, owner, to)
        persistExpense(insideWithExpense, owner, Instant.parse("2026-10-09T00:00:00Z"))
        val outsideWithExpense = persistEvent(owner, "Viaje anterior", Instant.parse("2026-10-09T12:00:00Z"))
        persistExpense(outsideWithExpense, owner, Instant.parse("2026-10-10T12:00:00Z"))
        persistEvent(owner, "Viaje posterior", Instant.parse("2026-10-11T00:00:00Z"))
    }

    @Test
    fun `countEventsCreated includes both window bounds`() {
        assertEquals(2, query.countEventsCreated(from, to))
    }

    @Test
    fun `countExpensesRegistered ignores expenses outside the window`() {
        assertEquals(2, query.countExpensesRegistered(from, to))
    }

    @Test
    fun `coverage counts every event that has at least one expense`() {
        assertEquals(2, query.countEventsWithAtLeastOneExpense())
        assertEquals(4, query.countEvents())
    }

    private fun persistEvent(
        owner: User,
        name: String,
        createdAt: Instant,
    ): Event {
        val event = Event.create(owner = owner, name = name, baseCurrency = "ARS")
        event.createdAt = createdAt
        return eventRepository.saveAndFlush(event)
    }

    private fun persistExpense(
        event: Event,
        owner: User,
        createdAt: Instant,
    ) {
        val member =
            eventMemberRepository.findAllByEventId(requireNotNull(event.id)).firstOrNull()
                ?: eventMemberRepository.saveAndFlush(
                    EventMember.create(event = event, displayName = "Ada", user = owner),
                )
        val expense =
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal("10.00"),
                originalCurrency = "ARS",
                exchangeRate = BigDecimal.ONE,
                expenseDate = LocalDate.parse("2026-10-10"),
            )
        expense.createdAt = createdAt
        expenseRepository.saveAndFlush(expense)
    }
}
