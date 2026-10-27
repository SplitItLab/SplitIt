package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.port.AuthGuest
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// SPT-98: a guest loads expenses paid by its own member, never on behalf of another.
class GuestExpenseApplicationServiceTest {
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()
    private val expenseService: ExpenseService = mock()
    private val exchangeRateProvider: ExchangeRateProvider = mock()

    private val expenseApplicationService =
        ExpenseApplicationService(
            eventAccessService = EventAccessService(eventService, eventMemberService),
            eventMemberService = eventMemberService,
            expenseService = expenseService,
            exchangeQuoteService = ExchangeQuoteService(exchangeRateProvider),
        )

    private val event =
        Event
            .create(owner = Helpers.user(name = "Mateo"), name = "Viaje a Bariloche", baseCurrency = "ARS")
            .apply { id = 10L }

    @Test
    fun `addExpense lets a guest load an expense paid by its own member`() {
        val juan = EventMember.create(event, "Juan").apply { id = 30L }
        val savedExpense =
            Expense
                .create(
                    event = event,
                    paidByMember = juan,
                    name = "Cena",
                    originalAmount = BigDecimal("5000"),
                    originalCurrency = "ARS",
                    expenseDate = LocalDate.now(),
                ).apply { id = 34L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(30L)).thenReturn(juan)
        whenever(expenseService.addExpense(any(), any(), any(), any(), any(), any())).thenReturn(savedExpense)

        val summary = expenseApplicationService.addExpense(guestCommand(paidByMemberId = 30L))

        assertEquals(34L, summary.id)
        assertEquals(30L, summary.paidByMember.id)
    }

    @Test
    fun `addExpense forbids a guest from loading an expense paid by another member`() {
        whenever(eventService.findById(10L)).thenReturn(event)

        assertFailsWith<AccessDeniedException> {
            expenseApplicationService.addExpense(guestCommand(paidByMemberId = 100L))
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    private fun guestCommand(paidByMemberId: Long) =
        CreateExpenseCommand(
            principal = AuthGuest(eventId = 10L, memberId = 30L, name = "Juan"),
            eventId = 10L,
            name = "Cena",
            amount = BigDecimal("5000"),
            currency = "ARS",
            paidByMemberId = paidByMemberId,
        )
}
