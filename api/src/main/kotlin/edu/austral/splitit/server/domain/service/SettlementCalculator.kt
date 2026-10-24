package edu.austral.splitit.server.domain.service

import java.math.BigDecimal

data class SuggestedPayment(
    val fromMemberId: Long,
    val toMemberId: Long,
    val amount: BigDecimal,
)

/**
 * Liquidación simplificada del prototipo: ordena deudores y acreedores por monto
 * y empareja el mayor contra el mayor hasta saldar todo. Lo que queda por debajo
 * de un centavo se considera saldado.
 */
object SettlementCalculator {
    private val NEGLIGIBLE_AMOUNT = BigDecimal("0.01")

    fun settle(netBalancesByMemberId: Map<Long, BigDecimal>): List<SuggestedPayment> {
        val debtors =
            netBalancesByMemberId
                .filterValues { it.signum() < 0 }
                .map { (memberId, balance) -> PendingAmount(memberId, balance.negate()) }
                .sortedByDescending { it.amount }
        val creditors =
            netBalancesByMemberId
                .filterValues { it.signum() > 0 }
                .map { (memberId, balance) -> PendingAmount(memberId, balance) }
                .sortedByDescending { it.amount }

        return match(debtors, creditors, emptyList())
    }

    private tailrec fun match(
        debtors: List<PendingAmount>,
        creditors: List<PendingAmount>,
        payments: List<SuggestedPayment>,
    ): List<SuggestedPayment> {
        if (debtors.isEmpty() || creditors.isEmpty()) return payments

        val debtor = debtors.first()
        val creditor = creditors.first()
        val amount = debtor.amount.min(creditor.amount)
        val nextPayments =
            if (amount < NEGLIGIBLE_AMOUNT) {
                payments
            } else {
                payments + SuggestedPayment(debtor.memberId, creditor.memberId, amount)
            }

        return match(debtors.afterPaying(amount), creditors.afterPaying(amount), nextPayments)
    }

    private fun List<PendingAmount>.afterPaying(amount: BigDecimal): List<PendingAmount> {
        val remaining = first().amount.subtract(amount)
        return if (remaining < NEGLIGIBLE_AMOUNT) {
            drop(1)
        } else {
            listOf(first().copy(amount = remaining)) + drop(1)
        }
    }

    private data class PendingAmount(
        val memberId: Long,
        val amount: BigDecimal,
    )
}
