package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SettlementApplicationServiceTest {
    private val balanceApplicationService: BalanceApplicationService = mock()

    private val settlementApplicationService =
        SettlementApplicationService(
            balanceApplicationService = balanceApplicationService,
        )

    private val authUser = Helpers.authUser(1L)

    @Test
    fun `listSuggestedPayments settles the member balances naming who pays and who receives`() {
        whenever(balanceApplicationService.listBalances(authUser, 10L)).thenReturn(
            listOf(
                BalanceSummary(memberId = 100L, memberName = "Ana", netBalance = BigDecimal("5000.0000")),
                BalanceSummary(memberId = 101L, memberName = "Juan", netBalance = BigDecimal("-2000.0000")),
                BalanceSummary(memberId = 102L, memberName = "Sofi", netBalance = BigDecimal("-3000.0000")),
            ),
        )

        val payments = settlementApplicationService.listSuggestedPayments(principal = authUser, eventId = 10L)

        assertEquals(
            listOf(
                SuggestedPaymentSummary(
                    fromMember = SuggestedPaymentMemberSummary(id = 102L, name = "Sofi"),
                    toMember = SuggestedPaymentMemberSummary(id = 100L, name = "Ana"),
                    amount = BigDecimal("3000.0000"),
                ),
                SuggestedPaymentSummary(
                    fromMember = SuggestedPaymentMemberSummary(id = 101L, name = "Juan"),
                    toMember = SuggestedPaymentMemberSummary(id = 100L, name = "Ana"),
                    amount = BigDecimal("2000.0000"),
                ),
            ),
            payments,
        )
    }

    @Test
    fun `listSuggestedPayments suggests nothing when every balance is zero`() {
        whenever(balanceApplicationService.listBalances(authUser, 10L)).thenReturn(
            listOf(
                BalanceSummary(memberId = 100L, memberName = "Ana", netBalance = BigDecimal("0.0000")),
                BalanceSummary(memberId = 101L, memberName = "Juan", netBalance = BigDecimal("0.0000")),
            ),
        )

        val payments = settlementApplicationService.listSuggestedPayments(principal = authUser, eventId = 10L)

        assertTrue(payments.isEmpty())
    }

    @Test
    fun `listSuggestedPayments propagates EventNotFoundException when event does not exist`() {
        whenever(balanceApplicationService.listBalances(authUser, 999L)).thenThrow(EventNotFoundException())

        assertFailsWith<EventNotFoundException> {
            settlementApplicationService.listSuggestedPayments(principal = authUser, eventId = 999L)
        }
    }

    @Test
    fun `listSuggestedPayments propagates AccessDeniedException when user is not a member`() {
        whenever(balanceApplicationService.listBalances(Helpers.authUser(2L), 10L))
            .thenThrow(AccessDeniedException("Forbidden"))

        assertFailsWith<AccessDeniedException> {
            settlementApplicationService.listSuggestedPayments(principal = Helpers.authUser(2L), eventId = 10L)
        }
    }
}
