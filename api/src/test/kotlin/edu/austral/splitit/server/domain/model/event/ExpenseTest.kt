package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.Helpers
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertEquals(BigDecimal("1.000000"), expense.exchangeRate)
        assertEquals(BigDecimal("5000.0000"), expense.baseAmount)
        assertEquals(LocalDate.of(2026, 9, 3), expense.expenseDate)
        assertNotNull(expense.createdAt)
        assertNotNull(expense.updatedAt)
    }

    @Test
    fun `create expense in another currency stores the conversion rounded`() {
        val expense =
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Taxi",
                originalAmount = BigDecimal("3.33"),
                originalCurrency = "USD",
                exchangeRate = BigDecimal("1523.96622"),
                expenseDate = LocalDate.of(2026, 9, 28),
            )

        assertEquals(BigDecimal("3.33"), expense.originalAmount)
        assertEquals("USD", expense.originalCurrency)
        assertEquals(BigDecimal("1523.966220"), expense.exchangeRate)
        assertEquals(BigDecimal("5074.8075"), expense.baseAmount)
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

    @Test
    fun `create expense fails when the converted base amount exceeds the column precision`() {
        // 1e12 USD × 1523.9662 = 1523966200000000 ARS: 16 integer digits, NUMERIC(19,4) allows 15
        assertFailsWith<IllegalArgumentException> {
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Compra grande",
                originalAmount = BigDecimal("1000000000000"),
                originalCurrency = "USD",
                exchangeRate = BigDecimal("1523.9662"),
                expenseDate = LocalDate.now(),
            )
        }
    }

    @Test
    fun `create expense accepts a converted base amount at the column precision limit`() {
        val expense =
            Expense.create(
                event = event,
                paidByMember = member,
                name = "Compra grande",
                originalAmount = BigDecimal("100000000000"),
                originalCurrency = "USD",
                exchangeRate = BigDecimal("9999"),
                expenseDate = LocalDate.now(),
            )

        assertEquals(BigDecimal("999900000000000.0000"), expense.baseAmount)
    }

    private fun cena() =
        Expense.create(
            event = event,
            paidByMember = member,
            name = "Cena",
            originalAmount = BigDecimal("5000"),
            originalCurrency = "ARS",
            expenseDate = LocalDate.of(2026, 9, 3),
        )

    @Test
    fun `update changes name, amount, currency and payer recalculating the base amount`() {
        val expense = cena()
        val otherMember = EventMember.create(event = event, displayName = "Ana")
        val previousUpdatedAt = expense.updatedAt

        expense.update(
            paidByMember = otherMember,
            name = "  Taxi  ",
            originalAmount = BigDecimal("10"),
            originalCurrency = "usd",
            exchangeRate = BigDecimal("1523.9662"),
        )

        assertEquals("Taxi", expense.name)
        assertEquals(otherMember, expense.paidByMember)
        assertEquals(BigDecimal("10"), expense.originalAmount)
        assertEquals("USD", expense.originalCurrency)
        assertEquals(BigDecimal("1523.966200"), expense.exchangeRate)
        assertEquals(BigDecimal("15239.6620"), expense.baseAmount)
        assertEquals(LocalDate.of(2026, 9, 3), expense.expenseDate)
        assertTrue(!expense.updatedAt.isBefore(previousUpdatedAt))
    }

    @Test
    fun `update fails with blank name or non positive amount`() {
        val expense = cena()

        assertFailsWith<IllegalArgumentException> {
            expense.update(member, "   ", BigDecimal("10"), "ARS", BigDecimal.ONE)
        }
        assertFailsWith<IllegalArgumentException> {
            expense.update(member, "Cena", BigDecimal.ZERO, "ARS", BigDecimal.ONE)
        }
        assertFailsWith<IllegalArgumentException> {
            expense.update(member, "Cena", BigDecimal("-1"), "ARS", BigDecimal.ONE)
        }
        assertEquals("Cena", expense.name)
        assertEquals(BigDecimal("5000"), expense.originalAmount)
    }

    @Test
    fun `update fails when the payer belongs to another event`() {
        val expense = cena()
        val otherEvent = Event.create(owner = owner, name = "Otro Evento", baseCurrency = "ARS")
        val otherMember = EventMember.create(event = otherEvent, displayName = "Otro")

        assertFailsWith<IllegalArgumentException> {
            expense.update(otherMember, "Cena", BigDecimal("10"), "ARS", BigDecimal.ONE)
        }
        assertEquals(member, expense.paidByMember)
    }

    @Test
    fun `update fails when the converted base amount exceeds the column precision`() {
        val expense = cena()

        assertFailsWith<IllegalArgumentException> {
            expense.update(member, "Compra grande", BigDecimal("1000000000000"), "USD", BigDecimal("1523.9662"))
        }
        assertEquals(BigDecimal("5000.0000"), expense.baseAmount)
    }
}
