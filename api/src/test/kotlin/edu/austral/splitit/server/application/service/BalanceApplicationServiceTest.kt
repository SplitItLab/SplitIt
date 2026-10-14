package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
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

class BalanceApplicationServiceTest {
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()
    private val expenseService: ExpenseService = mock()

    private val balanceApplicationService =
        BalanceApplicationService(
            eventService = eventService,
            eventMemberService = eventMemberService,
            expenseService = expenseService,
        )

    private val user = Helpers.user(name = "Ana", email = "ana@example.com", passwordHash = "hash")

    @Test
    fun `listBalances returns each current member net balance in member order`() {
        val event =
            Event
                .create(owner = user, name = "Asado", baseCurrency = "ARS")
                .apply { id = 10L }
        val ana = EventMember.create(event, "Ana", user).apply { id = 1L }
        val luis = EventMember.create(event, "Luis").apply { id = 2L }
        val expense =
            Expense.create(
                event = event,
                paidByMember = ana,
                name = "Carne",
                originalAmount = BigDecimal("100"),
                originalCurrency = "ARS",
                expenseDate = LocalDate.of(2024, 3, 16),
            )

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findByEventId(10L)).thenReturn(listOf(ana, luis))
        whenever(expenseService.findByEventId(10L)).thenReturn(listOf(expense))

        val balances = balanceApplicationService.listBalances(userId = 1L, eventId = 10L)

        assertEquals(
            listOf(
                BalanceSummary(memberId = 1L, memberName = "Ana", netBalance = BigDecimal("50.0000")),
                BalanceSummary(memberId = 2L, memberName = "Luis", netBalance = BigDecimal("-50.0000")),
            ),
            balances,
        )
    }

    @Test
    fun `listBalances throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            balanceApplicationService.listBalances(userId = 1L, eventId = 999L)
        }

        verify(eventMemberService, never()).findByEventId(any())
        verify(expenseService, never()).findByEventId(any())
    }

    @Test
    fun `listBalances throws AccessDeniedException when user is not owner nor member`() {
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
            balanceApplicationService.listBalances(userId = 1L, eventId = 30L)
        }

        verify(eventMemberService, never()).findByEventId(any())
        verify(expenseService, never()).findByEventId(any())
    }
}
