package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.exception.ExpenseNotFoundException
import edu.austral.splitit.server.application.port.ExchangeRateProvider
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.ExpenseService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertFailsWith

class DeleteExpenseApplicationServiceTest {
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()
    private val expenseService: ExpenseService = mock()
    private val exchangeRateProvider: ExchangeRateProvider = mock()

    private val expenseApplicationService =
        ExpenseApplicationService(
            eventService = eventService,
            eventMemberService = eventMemberService,
            expenseService = expenseService,
            exchangeQuoteService = ExchangeQuoteService(exchangeRateProvider),
        )

    private val user = Helpers.user(name = "Mateo", email = "mateo@example.com", passwordHash = "hash")

    private fun existingExpense(
        event: Event,
        payer: EventMember,
    ) = Expense
        .create(
            event = event,
            paidByMember = payer,
            name = "Cena",
            originalAmount = BigDecimal("5000"),
            originalCurrency = "ARS",
            expenseDate = LocalDate.now(),
        ).apply { id = 34L }

    @Test
    fun `deleteExpense deletes an expense of the event when requested by a member`() {
        val otherOwner =
            Helpers
                .user(name = "Otro", email = "otro@example.com", passwordHash = "hash")
                .apply { id = 2L }
        val event =
            Event
                .create(owner = otherOwner, name = "Asado", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val expense = existingExpense(event, payer)

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 1L)).thenReturn(true)
        whenever(expenseService.findById(34L)).thenReturn(expense)

        expenseApplicationService.deleteExpense(userId = 1L, eventId = 10L, expenseId = 34L)

        verify(expenseService).deleteExpense(expense)
    }

    @Test
    fun `deleteExpense throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            expenseApplicationService.deleteExpense(userId = 1L, eventId = 999L, expenseId = 34L)
        }

        verify(expenseService, never()).deleteExpense(any())
    }

    @Test
    fun `deleteExpense throws AccessDeniedException when user is not owner nor member`() {
        val otherOwner =
            Helpers
                .user(name = "Otro", email = "otro@example.com", passwordHash = "hash")
                .apply { id = 2L }
        val event =
            Event
                .create(owner = otherOwner, name = "Privado", baseCurrency = "ARS")
                .apply { id = 30L }

        whenever(eventService.findById(30L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(30L, 1L)).thenReturn(false)

        assertFailsWith<AccessDeniedException> {
            expenseApplicationService.deleteExpense(userId = 1L, eventId = 30L, expenseId = 34L)
        }

        verify(expenseService, never()).deleteExpense(any())
    }

    @Test
    fun `deleteExpense throws ExpenseNotFoundException when expense does not exist`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(expenseService.findById(999L)).thenReturn(null)

        assertFailsWith<ExpenseNotFoundException> {
            expenseApplicationService.deleteExpense(userId = 1L, eventId = 10L, expenseId = 999L)
        }

        verify(expenseService, never()).deleteExpense(any())
    }

    @Test
    fun `deleteExpense throws ExpenseNotFoundException when expense belongs to another event`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val otherEvent =
            Event
                .create(owner = user, name = "Otro evento", baseCurrency = "ARS")
                .apply { id = 11L }
        val otherPayer = EventMember.create(otherEvent, "Mateo", user).apply { id = 200L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(expenseService.findById(34L)).thenReturn(existingExpense(otherEvent, otherPayer))

        assertFailsWith<ExpenseNotFoundException> {
            expenseApplicationService.deleteExpense(userId = 1L, eventId = 10L, expenseId = 34L)
        }

        verify(expenseService, never()).deleteExpense(any())
    }
}
