package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import edu.austral.splitit.server.infrastructure.persistence.ExpenseRepository
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiActivityQuery
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
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
class BusinessKpiCalculationTest(
    @Autowired private val query: JpaKpiActivityQuery,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
    @Autowired private val expenseRepository: ExpenseRepository,
) {
    private val from = Instant.parse("2026-10-10T00:00:00Z")
    private val to = Instant.parse("2026-10-10T23:59:59Z")
    private val context = KpiCalculationContext(from, to)

    @BeforeEach
    fun seed() {
        val owner =
            userRepository.saveAndFlush(
                Helpers.user(name = "Ada Lovelace", email = "ada-kpi-calc@example.com", id = null),
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
    fun `events created per day returns the count inside the window`() {
        val kpi = EventsCreatedPerDayKpi(query)

        val value = kpi.calculate(context)

        assertEquals(KpiIds.EVENTS_CREATED_PER_DAY, kpi.id)
        assertEquals(KpiValue.Numeric(2.0), value)
    }

    @Test
    fun `expenses registered per day returns the count inside the window`() {
        val kpi = ExpensesRegisteredPerDayKpi(query)

        val value = kpi.calculate(context)

        assertEquals(KpiIds.EXPENSES_REGISTERED_PER_DAY, kpi.id)
        assertEquals(KpiValue.Numeric(2.0), value)
    }

    @Test
    fun `events with an expense returns the historical percentage`() {
        val kpi = EventsWithExpensePercentKpi(query)

        val value = kpi.calculate(context)

        assertEquals(KpiIds.EVENTS_WITH_EXPENSE_PERCENT, kpi.id)
        assertEquals(KpiValue.Numeric(50.0), value)
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
