package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.domain.service.SettlementCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class SuggestedPaymentMemberSummary(
    val id: Long,
    val name: String,
)

data class SuggestedPaymentSummary(
    val fromMember: SuggestedPaymentMemberSummary,
    val toMember: SuggestedPaymentMemberSummary,
    val amount: BigDecimal,
)

@Service
class SettlementApplicationService(
    private val balanceApplicationService: BalanceApplicationService,
) {
    @Transactional(readOnly = true)
    fun listSuggestedPayments(
        userId: Long,
        eventId: Long,
    ): List<SuggestedPaymentSummary> {
        val balances = balanceApplicationService.listBalances(userId, eventId)
        val membersById =
            balances.associate { balance ->
                balance.memberId to SuggestedPaymentMemberSummary(id = balance.memberId, name = balance.memberName)
            }

        return SettlementCalculator
            .settle(balances.associate { it.memberId to it.netBalance })
            .map { payment ->
                SuggestedPaymentSummary(
                    fromMember = membersById.getValue(payment.fromMemberId),
                    toMember = membersById.getValue(payment.toMemberId),
                    amount = payment.amount,
                )
            }
    }
}
