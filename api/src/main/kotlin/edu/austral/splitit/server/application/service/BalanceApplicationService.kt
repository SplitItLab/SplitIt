package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.AuthPrincipal
import edu.austral.splitit.server.domain.service.BalanceCalculator
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.ExpenseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class BalanceSummary(
    val memberId: Long,
    val memberName: String,
    val netBalance: BigDecimal,
)

@Service
class BalanceApplicationService(
    private val eventAccessService: EventAccessService,
    private val eventMemberService: EventMemberService,
    private val expenseService: ExpenseService,
) {
    @Transactional(readOnly = true)
    fun listBalances(
        principal: AuthPrincipal,
        eventId: Long,
    ): List<BalanceSummary> {
        eventAccessService.findEventFor(principal, eventId)

        val members = eventMemberService.findByEventId(eventId)
        val expenses = expenseService.findByEventId(eventId)

        return BalanceCalculator.calculate(members, expenses).map { balance ->
            BalanceSummary(
                memberId = balance.memberId,
                memberName = balance.memberName,
                netBalance = balance.netBalance,
            )
        }
    }
}
