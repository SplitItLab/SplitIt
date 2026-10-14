package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.SettlementCalculator
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

// TODO(SPT-83): borrar cuando mergee SPT-83; lo define BalanceApplicationService con este mismo shape.
data class BalanceSummary(
    val memberId: Long,
    val memberName: String,
    val netBalance: BigDecimal,
)

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
    // TODO(SPT-83): reemplazar por `private val balanceApplicationService: BalanceApplicationService`.
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
) {
    @Transactional(readOnly = true)
    fun listSuggestedPayments(
        userId: Long,
        eventId: Long,
    ): List<SuggestedPaymentSummary> {
        // TODO(SPT-83): reemplazar por `balanceApplicationService.listBalances(userId, eventId)`.
        val balances = fakeListBalances(userId, eventId)
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

    // TODO(SPT-83): borrar cuando mergee SPT-83. Imita a BalanceApplicationService.listBalances:
    // mismo chequeo de acceso (404/403) y mismo tipo de retorno, con saldos de prueba. Sin gastos
    // todos quedan en cero; con gastos, el primer integrante pagó todo y cada uno de los demás le
    // debe lo mismo.
    private fun fakeListBalances(
        userId: Long,
        eventId: Long,
    ): List<BalanceSummary> {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()
        val isMember = event.owner.id == userId || eventMemberService.isUserMemberOfEvent(eventId, userId)
        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        val members = eventMemberService.findByEventId(eventId)
        val hasExpenses = eventService.hasExpenses(eventId)
        val debtPerMember = BigDecimal("1000.0000")

        return members.mapIndexed { index, member ->
            val netBalance =
                when {
                    !hasExpenses -> BigDecimal.ZERO
                    index == 0 -> debtPerMember.multiply(BigDecimal(members.size - 1))
                    else -> debtPerMember.negate()
                }
            BalanceSummary(
                memberId = requireNotNull(member.id),
                memberName = member.displayName,
                netBalance = netBalance,
            )
        }
    }
}
