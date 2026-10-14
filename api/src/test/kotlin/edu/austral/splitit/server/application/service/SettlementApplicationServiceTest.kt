package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// TODO(SPT-83): cuando mergee SPT-83, mockear BalanceApplicationService.listBalances en lugar de
// los servicios de dominio que usa la fuente de saldos de prueba.
class SettlementApplicationServiceTest {
    private val eventService: EventService = mock()
    private val eventMemberService: EventMemberService = mock()

    private val settlementApplicationService =
        SettlementApplicationService(
            eventService = eventService,
            eventMemberService = eventMemberService,
        )

    private val owner = Helpers.user(name = "Ana", email = "ana@example.com", passwordHash = "hash")

    private val event =
        Event
            .create(owner = owner, name = "Asado", baseCurrency = "ARS")
            .apply { id = 10L }

    private val members =
        listOf(
            EventMember.create(event, "Ana", owner).apply { id = 100L },
            EventMember.create(event, "Juan", null).apply { id = 101L },
            EventMember.create(event, "Sofi", null).apply { id = 102L },
        )

    @Test
    fun `listSuggestedPayments settles the member balances naming who pays and who receives`() {
        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventService.hasExpenses(10L)).thenReturn(true)
        whenever(eventMemberService.findByEventId(10L)).thenReturn(members)

        val payments = settlementApplicationService.listSuggestedPayments(userId = 1L, eventId = 10L)

        assertEquals(
            listOf(
                SuggestedPaymentSummary(
                    fromMember = SuggestedPaymentMemberSummary(id = 101L, name = "Juan"),
                    toMember = SuggestedPaymentMemberSummary(id = 100L, name = "Ana"),
                    amount = BigDecimal("1000.0000"),
                ),
                SuggestedPaymentSummary(
                    fromMember = SuggestedPaymentMemberSummary(id = 102L, name = "Sofi"),
                    toMember = SuggestedPaymentMemberSummary(id = 100L, name = "Ana"),
                    amount = BigDecimal("1000.0000"),
                ),
            ),
            payments,
        )
    }

    @Test
    fun `listSuggestedPayments suggests nothing when every balance is zero`() {
        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventService.hasExpenses(10L)).thenReturn(false)
        whenever(eventMemberService.findByEventId(10L)).thenReturn(members)

        val payments = settlementApplicationService.listSuggestedPayments(userId = 1L, eventId = 10L)

        assertTrue(payments.isEmpty())
    }

    @Test
    fun `listSuggestedPayments lets a member who is not the owner see the payments`() {
        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(true)
        whenever(eventService.hasExpenses(10L)).thenReturn(true)
        whenever(eventMemberService.findByEventId(10L)).thenReturn(members)

        val payments = settlementApplicationService.listSuggestedPayments(userId = 2L, eventId = 10L)

        assertEquals(2, payments.size)
    }

    @Test
    fun `listSuggestedPayments throws EventNotFoundException when event does not exist`() {
        whenever(eventService.findById(999L)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            settlementApplicationService.listSuggestedPayments(userId = 1L, eventId = 999L)
        }

        verify(eventMemberService, never()).findByEventId(any())
    }

    @Test
    fun `listSuggestedPayments throws AccessDeniedException when user is not a member`() {
        whenever(eventService.findById(10L)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(false)

        assertFailsWith<AccessDeniedException> {
            settlementApplicationService.listSuggestedPayments(userId = 2L, eventId = 10L)
        }

        verify(eventMemberService, never()).findByEventId(any())
    }
}
