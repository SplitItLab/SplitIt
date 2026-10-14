package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.service.BalanceSummary
import java.math.BigDecimal

data class BalanceResponse(
    val memberId: Long,
    val memberName: String,
    val netBalance: BigDecimal,
) {
    companion object {
        fun of(summary: BalanceSummary): BalanceResponse =
            BalanceResponse(
                memberId = summary.memberId,
                memberName = summary.memberName,
                netBalance = summary.netBalance,
            )
    }
}
