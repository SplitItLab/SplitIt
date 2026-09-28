package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.ExpenseService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
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

class EventApplicationServiceExpenseTest {
    private val userService: UserService = mock()
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()
    private val inviteLinkService: InviteLinkService = mock()
    private val expenseService: ExpenseService = mock()

    private val eventApplicationService =
        EventApplicationService(
            userService = userService,
            eventService = eventService,
            eventMemberService = eventMemberService,
            inviteLinkService = inviteLinkService,
            expenseService = expenseService,
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
            ),
        ).thenReturn(savedExpense)

        val summary =
            eventApplicationService.addExpense(
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
        assertEquals(BigDecimal("5000"), summary.baseAmount)
        assertEquals("ARS", summary.baseCurrency)
        assertEquals(100L, summary.paidByMember.id)
        assertEquals("Mateo", summary.paidByMember.name)
    }

    @Test
    fun `addExpense throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            eventApplicationService.addExpense(
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

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any())
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
            eventApplicationService.addExpense(
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

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any())
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
            eventApplicationService.addExpense(
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

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any())
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
            eventApplicationService.addExpense(
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

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any())
    }

    @Test
    fun `addExpense rejects a currency that does not match the event base currency`() {
        val event =
            Event
                .create(owner = user, name = "Viaje a Bariloche", baseCurrency = "ARS")
                .apply { id = 10L }
        val payer = EventMember.create(event, "Mateo", user).apply { id = 100L }

        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.findById(100L)).thenReturn(payer)

        assertFailsWith<IllegalArgumentException> {
            eventApplicationService.addExpense(
                CreateExpenseCommand(
                    userId = 1L,
                    eventId = 10L,
                    name = "Cena",
                    amount = BigDecimal("5000"),
                    currency = "USD",
                    paidByMemberId = 100L,
                ),
            )
        }

        verify(expenseService, never()).addExpense(any(), any(), any(), any(), any())
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

        val result = eventApplicationService.listExpenses(userId = 1L, eventId = 20L)

        assertEquals(1, result.size)
        assertEquals(34L, result[0].id)
        assertEquals("Cena", result[0].name)
        assertEquals(100L, result[0].paidByMember.id)
        assertEquals("Mateo", result[0].paidByMember.name)
    }

    @Test
    fun `listExpenses throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            eventApplicationService.listExpenses(userId = 1L, eventId = 999L)
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
            eventApplicationService.listExpenses(userId = 1L, eventId = 30L)
        }

        verify(expenseService, never()).findByEventId(any())
    }
}
