package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.service.ExpenseCursor
import edu.austral.splitit.server.domain.service.ExpenseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(ExpenseService::class)
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
class ExpenseRepositoryPaginationTest(
    @Autowired private val expenseService: ExpenseService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
    @Autowired private val expenseRepository: ExpenseRepository,
) {
    private var eventId = 0L
    private var otherEventId = 0L

    @BeforeEach
    fun seed() {
        val owner = userRepository.saveAndFlush(Helpers.user(name = "Ada", email = "ada-page@example.com", id = null))
        val event = eventRepository.saveAndFlush(Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS"))
        val other = eventRepository.saveAndFlush(Event.create(owner = owner, name = "Otro", baseCurrency = "ARS"))
        eventId = requireNotNull(event.id)
        otherEventId = requireNotNull(other.id)

        // 5 expenses over 3 days; two share a day so the id breaks the tie.
        persistExpense(event, owner.name, "A", "100", LocalDate.of(2026, 10, 1))
        persistExpense(event, owner.name, "B", "200", LocalDate.of(2026, 10, 2))
        persistExpense(event, owner.name, "C", "300", LocalDate.of(2026, 10, 2))
        persistExpense(event, owner.name, "D", "400", LocalDate.of(2026, 10, 3))
        persistExpense(event, owner.name, "E", "500", LocalDate.of(2026, 10, 3))
        persistExpense(other, owner.name, "Ajeno", "9999", LocalDate.of(2026, 10, 4))
    }

    @Test
    fun `walking the pages returns every expense once, newest first`() {
        val seen = mutableListOf<String>()
        var cursor: ExpenseCursor? = null
        do {
            val page = expenseService.findPageByEventId(eventId, 3, cursor)
            seen += page.take(2).map { it.name }
            cursor = page.take(2).lastOrNull()?.let { ExpenseCursor(it.expenseDate, requireNotNull(it.id)) }
        } while (page.size > 2)

        assertEquals(listOf("E", "D", "C", "B", "A"), seen)
    }

    @Test
    fun `the next page is not shifted by an expense deleted or added after the cursor`() {
        val firstPage = expenseService.findPageByEventId(eventId, 2, null)
        val cursor = firstPage.last().let { ExpenseCursor(it.expenseDate, requireNotNull(it.id)) }

        expenseService.deleteExpense(firstPage.first())
        expenseRepository.flush()

        assertEquals(listOf("C", "B"), expenseService.findPageByEventId(eventId, 2, cursor).map { it.name })
    }

    @Test
    fun `total adds up every expense of the event, not just a page`() {
        assertEquals(5L, expenseService.countByEventId(eventId))
        assertEquals(0, BigDecimal("1500").compareTo(expenseService.totalBaseAmountByEventId(eventId)))
        assertEquals(0, BigDecimal("9999").compareTo(expenseService.totalBaseAmountByEventId(otherEventId)))
    }

    private fun persistExpense(
        event: Event,
        payerName: String,
        name: String,
        amount: String,
        date: LocalDate,
    ) {
        val payer =
            eventMemberRepository.findAllByEventId(requireNotNull(event.id)).firstOrNull()
                ?: eventMemberRepository.saveAndFlush(EventMember.create(event, payerName, null))
        expenseRepository.saveAndFlush(
            Expense.create(
                event = event,
                paidByMember = payer,
                name = name,
                originalAmount = BigDecimal(amount),
                originalCurrency = "ARS",
                expenseDate = date,
            ),
        )
    }
}
