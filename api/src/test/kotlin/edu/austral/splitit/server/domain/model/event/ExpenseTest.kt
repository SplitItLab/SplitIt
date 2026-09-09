package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.Helpers
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ExpenseTest {
    private val owner = Helpers.user(name = "Dueño", email = "dueno@example.com", passwordHash = "hash")
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
                expenseDate = LocalDate.now(),
            )
        }
    }

    @Test
    fun `create expense fails if paidByMember belongs to a different unsaved event`() {
        val otherEvent = Event.create(owner = owner, name = "Otro Evento", baseCurrency = "USD")
        val otherMember = EventMember.create(event = otherEvent, displayName = "Otro")

        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = otherMember,
                name = "Cena",
                originalAmount = BigDecimal("100.00"),
                originalCurrency = "USD",
                expenseDate = LocalDate.now(),
            )
        }
    }

    @Test
    fun `create expense fails when currency is not three letters`() {
        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal("100.00"),
                originalCurrency = "123",
                expenseDate = LocalDate.now(),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal("100.00"),
                originalCurrency = "AR",
                expenseDate = LocalDate.now(),
            )
        }
    }

    @Test
    fun `create expense fails when amounts exceed column scale or precision`() {
        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal("1.23456"),
                originalCurrency = "ARS",
                expenseDate = LocalDate.now(),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = BigDecimal("1.23"),
                originalCurrency = "ARS",
                exchangeRate = BigDecimal("1.1234567"),
                expenseDate = LocalDate.now(),
            )
        }

        val tooManyIntegerDigits = BigDecimal("1".repeat(16))
        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Cena",
                originalAmount = tooManyIntegerDigits,
                originalCurrency = "ARS",
                expenseDate = LocalDate.now(),
            )
        }
    }
}
