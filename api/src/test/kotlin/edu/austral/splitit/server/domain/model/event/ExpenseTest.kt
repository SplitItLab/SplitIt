package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.user.User
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ExpenseTest {
    private val owner = User(id = 1L, name = "Dueño", email = "dueno@example.com", passwordHash = "hash")
    private val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")
    private val member = EventMember.create(event = event, displayName = "Dueño", user = owner)

    @Test
    fun `create valid expense`() {
        val expense =
            Expense.create(
                event = event,
                paidByMember = member,
                name = "  Cena  ",
                originalAmount = BigDecimal("5000.00"),
                originalCurrency = "ars",
                exchangeRate = BigDecimal.ONE,
                baseAmount = BigDecimal("5000.00"),
                expenseDate = LocalDate.of(2026, 9, 3),
            )

        assertEquals("Cena", expense.name)
        assertEquals(event, expense.event)
        assertEquals(member, expense.paidByMember)
        assertEquals(BigDecimal("5000.00"), expense.originalAmount)
        assertEquals("ARS", expense.originalCurrency)
        assertEquals(BigDecimal.ONE, expense.exchangeRate)
        assertEquals(BigDecimal("5000.00"), expense.baseAmount)
        assertEquals(LocalDate.of(2026, 9, 3), expense.expenseDate)
        assertNotNull(expense.createdAt)
        assertNotNull(expense.updatedAt)
    }

    @Test
    fun `create expense fails if originalAmount is zero or negative`() {
        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal.ZERO,
                originalCurrency = "ARS",
                baseAmount = BigDecimal.ZERO,
                expenseDate = LocalDate.now(),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal("-10.00"),
                originalCurrency = "ARS",
                baseAmount = BigDecimal("10.00"),
                expenseDate = LocalDate.now(),
            )
        }
    }

    @Test
    fun `create expense fails if paidByMember belongs to another event`() {
        val otherEvent = Event.create(owner = owner, name = "Otro Evento", baseCurrency = "USD")
        otherEvent.id = 99L
        event.id = 1L

        val otherMember = EventMember.create(event = otherEvent, displayName = "Otro")
        otherMember.id = 42L

        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = otherMember,
                name = "Cena",
                originalAmount = BigDecimal("100.00"),
                originalCurrency = "USD",
                baseAmount = BigDecimal("100.00"),
                expenseDate = LocalDate.now(),
            )
        }
    }
}
