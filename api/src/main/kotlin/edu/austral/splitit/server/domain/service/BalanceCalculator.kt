package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import java.math.BigDecimal
import java.math.RoundingMode

data class MemberBalance(
    val memberId: Long,
    val memberName: String,
    val netBalance: BigDecimal,
)

object BalanceCalculator {
    private const val SCALE = 4

    fun calculate(
        members: List<EventMember>,
        expenses: List<Expense>,
    ): List<MemberBalance> {
        if (members.isEmpty()) return emptyList()

        val share = total(expenses).divide(BigDecimal(members.size), SCALE, RoundingMode.HALF_UP)
        val paidByMemberId = expenses.groupBy { requireNotNull(it.paidByMember.id) }

        return members.map { member ->
            val paid = total(paidByMemberId[member.id].orEmpty())
            MemberBalance(
                memberId = requireNotNull(member.id),
                memberName = member.displayName,
                netBalance = paid.subtract(share).setScale(SCALE, RoundingMode.HALF_UP),
            )
        }
    }

    private fun total(expenses: List<Expense>): BigDecimal =
        expenses.fold(BigDecimal.ZERO) { sum, expense ->
            sum.add(expense.baseAmount)
        }
}
