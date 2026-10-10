package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.exception.ExchangeRateUnavailableException
import edu.austral.splitit.server.application.exception.ExpenseNotFoundException
import edu.austral.splitit.server.application.port.ExchangeRateProvider
import edu.austral.splitit.server.domain.model.event.Currency
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

class ExpenseApplicationServiceTest {
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

    @Test
    fun `addExpense creates expense in base currency when requested by owner`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val savedExpense =
            Expense
                .create(
                    event = event,
                    paidByMember = payer,
                    name = "Cena",
                    originalAmount = BigDecimal("5000"),
                    originalCurrency = "ARS",
                    expenseDate = LocalDate.now(),
                ).apply { id = 34L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(100L)).thenReturn(payer)
        whenever(
            expenseService.addExpense(
                event = event,
                paidByMember = payer,
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "ARS",
                exchangeRate = BigDecimal("1.000000"),
            ),
        ).thenReturn(savedExpense)

        val summary =
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "ARS",
                    paidByMemberId = 100L,
                ),
            )

        assertEquals(34L, summary.id)
        assertEquals(10L, summary.eventId)
        assertEquals("Cena", summary.name)
        assertEquals(BigDecimal("5000"), summary.originalAmount)
        assertEquals("ARS", summary.originalCurrency)
        assertEquals(0, BigDecimal.ONE.compareTo(summary.exchangeRate))
        assertEquals(0, BigDecimal("5000").compareTo(summary.baseAmount))
        assertEquals("ARS", summary.baseCurrency)
        assertEquals(100L, summary.paidByMember.id)
        assertEquals("Mateo", summary.paidByMember.name)
        verify(exchangeRateProvider, never()).rate(any(), any())
    }

    @Test
    fun `addExpense throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 999L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "ARS",
                    paidByMemberId = 100L,
                ),
            )
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `addExpense throws AccessDeniedException when user is not owner nor member`() {
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
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 30L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "ARS",
                    paidByMemberId = 100L,
                ),
            )
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `addExpense rejects a payer that does not belong to the event`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val otherEvent =
            Event
                .create(owner = user, name = "Otro evento", baseCurrency = "ARS")
                .apply { id = 11L }
        val payerFromOtherEvent = EventMember.create(otherEvent, "Ana", null).apply { id = 200L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(200L)).thenReturn(payerFromOtherEvent)

        assertFailsWith<IllegalArgumentException> {
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "ARS",
                    paidByMemberId = 200L,
                ),
            )
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `addExpense rejects a payer id that does not exist`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(999L)).thenReturn(null)

        assertFailsWith<IllegalArgumentException> {
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "ARS",
                    paidByMemberId = 999L,
                ),
            )
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `addExpense converts an expense in another currency using a server side quote`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val savedExpense =
            Expense
                .create(
                    event = event,
                    paidByMember = payer,
                    name = "Taxi",
                    originalAmount = BigDecimal("10"),
                    originalCurrency = "USD",
                    exchangeRate = BigDecimal("1523.966200"),
                    expenseDate = LocalDate.now(),
                ).apply { id = 34L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(100L)).thenReturn(payer)
        whenever(exchangeRateProvider.rate(Currency("USD"), Currency("ARS"))).thenReturn(BigDecimal("1523.9662"))
        whenever(
            expenseService.addExpense(
                event = event,
                paidByMember = payer,
                name = "Taxi",
                amount = BigDecimal("10"),
                currency = "USD",
                exchangeRate = BigDecimal("1523.966200"),
            ),
        ).thenReturn(savedExpense)

        val summary =
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Taxi",
                    amount = BigDecimal("10"),
                    currency = "USD",
                    paidByMemberId = 100L,
                ),
            )

        assertEquals("USD", summary.originalCurrency)
        assertEquals("ARS", summary.baseCurrency)
        assertEquals(BigDecimal("1523.966200"), summary.exchangeRate)
        assertEquals(BigDecimal("15239.6620"), summary.baseAmount)
    }

    @Test
    fun `addExpense does not save the expense when the exchange rate provider fails`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(100L)).thenReturn(payer)
        whenever(exchangeRateProvider.rate(any(), any())).thenThrow(ExchangeRateUnavailableException())

        assertFailsWith<ExchangeRateUnavailableException> {
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Taxi",
                    amount = BigDecimal("10"),
                    currency = "USD",
                    paidByMemberId = 100L,
                ),
            )
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `addExpense rejects an unsupported currency without calling the provider`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(100L)).thenReturn(payer)

        assertFailsWith<IllegalArgumentException> {
            expenseApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "GBP",
                    paidByMemberId = 100L,
                ),
            )
        }

        verify(exchangeRateProvider, never()).rate(any(), any())

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `listExpenses returns expenses when requested by a member`() {
        val otherOwner =
            Helpers
                .user(name = "Otro", email = "otro@example.com", passwordHash = "hash")
                .apply { id = 2L }
        val event =
            Event
                .create(owner = otherOwner, name = "Asado", baseCurrency = "ARS")
                .apply { id = 20L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
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

        whenever(eventService.findById(20L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(20L, 1L)).thenReturn(true)
        whenever(expenseService.findByEventId(20L)).thenReturn(listOf(expense))

        val result = expenseApplicationService.listExpenses(userId = 1L, eventId = 20L)

        assertEquals(1, result.size)
        assertEquals(34L, result[0].id)
        assertEquals("Cena", result[0].name)
        assertEquals(0, BigDecimal.ONE.compareTo(result[0].exchangeRate))
        assertEquals(100L, result[0].paidByMember.id)
        assertEquals("Mateo", result[0].paidByMember.name)
    }

    @Test
    fun `listExpenses throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            expenseApplicationService.listExpenses(userId = 1L, eventId = 999L)
        }

        verify(expenseService, never()).findByEventId(any())
    }

    @Test
    fun `listExpenses throws AccessDeniedException when user is not owner nor member`() {
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
            expenseApplicationService.listExpenses(userId = 1L, eventId = 30L)
        }

        verify(expenseService, never()).findByEventId(any())
    }

    @Test
    fun `quoteExpense converts the amount into the event base currency`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(exchangeRateProvider.rate(Currency("USD"), Currency("ARS"))).thenReturn(BigDecimal("1523.9662"))

        val quote =
            expenseApplicationService.quoteExpense(
                QuoteExpenseQuery(userId = 1L, eventId = 10L, amount = BigDecimal("10"), currency = "usd"),
            )

        assertEquals(BigDecimal("10"), quote.originalAmount)
        assertEquals("USD", quote.originalCurrency.get())
        assertEquals(BigDecimal("1523.966200"), quote.exchangeRate)
        assertEquals(BigDecimal("15239.6620"), quote.baseAmount)
        assertEquals("ARS", quote.baseCurrency.get())
    }

    @Test
    fun `quoteExpense throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            expenseApplicationService.quoteExpense(
                QuoteExpenseQuery(userId = 1L, eventId = 999L, amount = BigDecimal("10"), currency = "USD"),
            )
        }

        verify(exchangeRateProvider, never()).rate(any(), any())
    }

    @Test
    fun `quoteExpense throws AccessDeniedException when user is not owner nor member`() {
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
            expenseApplicationService.quoteExpense(
                QuoteExpenseQuery(userId = 1L, eventId = 30L, amount = BigDecimal("10"), currency = "USD"),
            )
        }

        verify(exchangeRateProvider, never()).rate(any(), any())
    }

    private fun updateCommand(
        eventId: Long = 10L,
        expenseId: Long = 34L,
        name: String = "Taxi",
        amount: BigDecimal = BigDecimal("10"),
        currency: String = "USD",
        paidByMemberId: Long = 101L,
    ) = UpdateExpenseCommand(
        userId = 1L,
        eventId = eventId,
        expenseId = expenseId,
        name = name,
        amount = amount,
        currency = currency,
        paidByMemberId = paidByMemberId,
    )

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
    fun `updateExpense changes name, amount, currency and payer quoting against the base currency`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val newPayer = EventMember.create(event, "Ana", null).apply { id = 101L }
        val expense = existingExpense(event, payer)

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(expenseService.findById(34L)).thenReturn(expense)
        whenever(eventMemberService.findById(101L)).thenReturn(newPayer)
        whenever(exchangeRateProvider.rate(Currency("USD"), Currency("ARS"))).thenReturn(BigDecimal("1523.9662"))
        whenever(
            expenseService.updateExpense(
                expense = expense,
                paidByMember = newPayer,
                name = "Taxi",
                amount = BigDecimal("10"),
                currency = "USD",
                exchangeRate = BigDecimal("1523.966200"),
            ),
        ).thenAnswer {
            expense.update(newPayer, "Taxi", BigDecimal("10"), "USD", BigDecimal("1523.966200"))
        }

        val summary = expenseApplicationService.updateExpense(updateCommand())

        assertEquals(34L, summary.id)
        assertEquals(10L, summary.eventId)
        assertEquals("Taxi", summary.name)
        assertEquals(BigDecimal("10"), summary.originalAmount)
        assertEquals("USD", summary.originalCurrency)
        assertEquals(BigDecimal("1523.966200"), summary.exchangeRate)
        assertEquals(BigDecimal("15239.6620"), summary.baseAmount)
        assertEquals("ARS", summary.baseCurrency)
        assertEquals(101L, summary.paidByMember.id)
        assertEquals("Ana", summary.paidByMember.name)
    }

    @Test
    fun `updateExpense in the base currency does not call the exchange rate provider`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val expense = existingExpense(event, payer)

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(expenseService.findById(34L)).thenReturn(expense)
        whenever(eventMemberService.findById(100L)).thenReturn(payer)
        whenever(
            expenseService.updateExpense(
                expense = expense,
                paidByMember = payer,
                name = "Cena larga",
                amount = BigDecimal("7000"),
                currency = "ARS",
                exchangeRate = BigDecimal("1.000000"),
            ),
        ).thenAnswer {
            expense.update(payer, "Cena larga", BigDecimal("7000"), "ARS", BigDecimal.ONE)
        }

        val summary =
            expenseApplicationService.updateExpense(
                updateCommand(
                    name = "Cena larga",
                    amount = BigDecimal("7000"),
                    currency = "ARS",
                    paidByMemberId = 100L,
                ),
            )

        assertEquals("Cena larga", summary.name)
        assertEquals(0, BigDecimal("7000").compareTo(summary.baseAmount))
        verify(exchangeRateProvider, never()).rate(any(), any())
    }

    @Test
    fun `updateExpense throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            expenseApplicationService.updateExpense(updateCommand(eventId = 999L))
        }

        verify(expenseService, never()).updateExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `updateExpense throws AccessDeniedException when user is not owner nor member`() {
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
            expenseApplicationService.updateExpense(updateCommand(eventId = 30L))
        }

        verify(expenseService, never()).updateExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `updateExpense throws ExpenseNotFoundException when expense does not exist`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(expenseService.findById(999L)).thenReturn(null)

        assertFailsWith<ExpenseNotFoundException> {
            expenseApplicationService.updateExpense(updateCommand(expenseId = 999L))
        }

        verify(expenseService, never()).updateExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `updateExpense throws ExpenseNotFoundException when expense belongs to another event`() {
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
            expenseApplicationService.updateExpense(updateCommand())
        }

        verify(expenseService, never()).updateExpense(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `updateExpense rejects a payer that does not belong to the event`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val otherEvent =
            Event
                .create(owner = user, name = "Otro evento", baseCurrency = "ARS")
                .apply { id = 11L }
        val payerFromOtherEvent = EventMember.create(otherEvent, "Ana", null).apply { id = 200L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(expenseService.findById(34L)).thenReturn(existingExpense(event, payer))
        whenever(eventMemberService.findById(200L)).thenReturn(payerFromOtherEvent)

        assertFailsWith<IllegalArgumentException> {
            expenseApplicationService.updateExpense(updateCommand(paidByMemberId = 200L))
        }

        verify(expenseService, never()).updateExpense(any(), any(), any(), any(), any(), any())
    }
}
