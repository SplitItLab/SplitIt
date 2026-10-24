package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.service.SuggestedPaymentSummary
import java.math.BigDecimal

data class SuggestedPaymentMemberResponse(
    val id: Long,
    val name: String,
)

data class SuggestedPaymentResponse(
    val fromMember: SuggestedPaymentMemberResponse,
    val toMember: SuggestedPaymentMemberResponse,
    val amount: BigDecimal,
) {
    companion object {
        fun of(summary: SuggestedPaymentSummary): SuggestedPaymentResponse =
            SuggestedPaymentResponse(
                fromMember =
                    SuggestedPaymentMemberResponse(
                        id = summary.fromMember.id,
                        name = summary.fromMember.name,
                    ),
                toMember =
                    SuggestedPaymentMemberResponse(
                        id = summary.toMember.id,
                        name = summary.toMember.name,
                    ),
                amount = summary.amount,
            )
    }
}
