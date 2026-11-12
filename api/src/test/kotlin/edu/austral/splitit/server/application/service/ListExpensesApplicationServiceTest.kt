package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.ExchangeRateProvider
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.ExpenseService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListExpensesApplicationServiceTest {
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

    private val authUser = Helpers.authUser(1L)

    private val user = Helpers.user(name = "Mateo", email = "mateo@example.com", passwordHash = "hash")

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
        whenever(expenseService.findPageByEventId(20L, 21, null)).thenReturn(listOf(expense))
        whenever(expenseService.totalBaseAmountByEventId(20L)).thenReturn(BigDecimal("5000"))
        whenever(expenseService.countByEventId(20L)).thenReturn(1L)

        val result =
            expenseApplicationService.listExpenses(principal = authUser, eventId = 20L, size = 20, after = null)

        assertEquals(1, result.items.size)
        assertEquals(34L, result.items[0].id)
        assertEquals("Cena", result.items[0].name)
        assertEquals(0, BigDecimal.ONE.compareTo(result.items[0].exchangeRate))
        assertEquals(100L, result.items[0].paidByMember.id)
        assertEquals("Mateo", result.items[0].paidByMember.name)
        assertEquals(false, result.hasNext)
        assertEquals(0, BigDecimal("5000").compareTo(result.totalAmount))
    }

    @Test
    fun `listExpenses returns one page, flags that there is more and reports the total of all expenses`() {
        val event =
            Event
                .create(owner = user, name = "Asado", baseCurrency = "ARS")
                .apply { id = 20L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }
        val expenses =
            (3L downTo 1L).map { expenseId ->
                Expense
                    .create(
                        event = event,
                        paidByMember = payer,
                        name = "Gasto $expenseId",
                        originalAmount = BigDecimal("100"),
                        originalCurrency = "ARS",
                        expenseDate = LocalDate.now(),
                    ).apply { id = expenseId }
            }

        whenever(eventService.findById(20L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(20L, 1L)).thenReturn(true)
        // The service asks for size + 1 rows to know whether another page exists.
        whenever(expenseService.findPageByEventId(20L, 3, null)).thenReturn(expenses)
        whenever(expenseService.totalBaseAmountByEventId(20L)).thenReturn(BigDecimal("900"))
        whenever(expenseService.countByEventId(20L)).thenReturn(3L)

        val result = expenseApplicationService.listExpenses(principal = authUser, eventId = 20L, size = 2, after = null)

        assertEquals(listOf(3L, 2L), result.items.map { it.id })
        assertEquals(true, result.hasNext)
        assertEquals(3L, result.totalCount)
        assertEquals(0, BigDecimal("900").compareTo(result.totalAmount))
    }

    @Test
    fun `listExpenses throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            expenseApplicationService.listExpenses(principal = authUser, eventId = 999L, size = 20, after = null)
        }

        verify(expenseService, never()).findPageByEventId(any(), any(), anyOrNull())
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
            expenseApplicationService.listExpenses(principal = authUser, eventId = 30L, size = 20, after = null)
        }

        verify(expenseService, never()).findPageByEventId(any(), any(), anyOrNull())
    }
}
