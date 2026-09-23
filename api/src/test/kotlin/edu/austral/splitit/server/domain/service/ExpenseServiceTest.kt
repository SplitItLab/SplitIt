package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.infrastructure.persistence.ExpenseRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ExpenseServiceTest {
    private val expenseRepository: ExpenseRepository = mock()
    private val expenseService = ExpenseService(expenseRepository)

    private val owner = Helpers.user(name = "Mateo", email = "mateo@example.com", passwordHash = "hash")
    private val event =
        Event
            .create(owner = owner, name = "Viaje a Bariloche", baseCurrency = "ARS")
            .apply { id = 10L }
    private val payer = EventMember.create(event, "Mateo", owner).apply { id = 100L }

    private fun echoSavedExpense() {
        whenever(expenseRepository.save(any<Expense>())).thenAnswer { it.getArgument<Expense>(0) }
    }

    @Test
    fun `addExpense creates and saves an expense with today's date`() {
        echoSavedExpense()

        val expense =
            expenseService.addExpense(
                event = event,
                paidByMember = payer,
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "ARS",
            )

        assertEquals("Cena", expense.name)
        assertEquals(BigDecimal("5000"), expense.originalAmount)
        assertEquals("ARS", expense.originalCurrency)
        assertEquals(BigDecimal("5000"), expense.baseAmount)
        assertSame(event, expense.event)
        assertSame(payer, expense.paidByMember)
        verify(expenseRepository).save(any<Expense>())
    }

    @Test
    fun `addExpense rejects a payer that belongs to a different event`() {
        val otherEvent =
            Event
                .create(owner = owner, name = "Otro evento", baseCurrency = "ARS")
                .apply { id = 20L }
        val otherPayer = EventMember.create(otherEvent, "Ana", null).apply { id = 200L }

        assertFailsWith<IllegalArgumentException> {
            expenseService.addExpense(
                event = event,
                paidByMember = otherPayer,
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "ARS",
            )
        }
    }

    @Test
    fun `findByEventId delegates to the deterministic ordered query`() {
        val expense =
            Expense
                .create(
                    event = event,
                    paidByMember = payer,
                    name = "Cena",
                    originalAmount = BigDecimal("5000"),
                    originalCurrency = "ARS",
                    expenseDate = LocalDate.now(),
                ).apply { id = 34L }
        whenever(expenseRepository.findAllOrderedByEventId(10L)).thenReturn(listOf(expense))

        val result = expenseService.findByEventId(10L)

        assertEquals(listOf(expense), result)
    }
}
