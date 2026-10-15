package edu.austral.splitit.server.domain.service

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementCalculatorTest {
    private fun payment(
        from: Long,
        to: Long,
        amount: String,
    ) = SuggestedPayment(fromMemberId = from, toMemberId = to, amount = BigDecimal(amount))

    private fun receivedBy(
        memberId: Long,
        payments: List<SuggestedPayment>,
    ): BigDecimal =
        payments
            .filter { it.toMemberId == memberId }
            .fold(BigDecimal.ZERO) { sum, payment -> sum.add(payment.amount) }

    private fun paidBy(
        memberId: Long,
        payments: List<SuggestedPayment>,
    ): BigDecimal =
        payments
            .filter { it.fromMemberId == memberId }
            .fold(BigDecimal.ZERO) { sum, payment -> sum.add(payment.amount) }

    @Test
    fun `a single debt becomes a single payment from the debtor to the creditor`() {
        val payments =
            SettlementCalculator.settle(
                mapOf(
                    1L to BigDecimal("500.0000"),
                    2L to BigDecimal("-500.0000"),
                ),
            )

        assertEquals(listOf(payment(from = 2L, to = 1L, amount = "500.0000")), payments)
    }

    @Test
    fun `several debtors pay one creditor starting with the largest debt`() {
        val payments =
            SettlementCalculator.settle(
                mapOf(
                    1L to BigDecimal("21925"),
                    2L to BigDecimal("-8275"),
                    3L to BigDecimal("-10675"),
                    4L to BigDecimal("-2975"),
                ),
            )

        assertEquals(
            listOf(
                payment(from = 3L, to = 1L, amount = "10675"),
                payment(from = 2L, to = 1L, amount = "8275"),
                payment(from = 4L, to = 1L, amount = "2975"),
            ),
            payments,
        )
    }

    @Test
    fun `a debtor splits the payment when a single creditor cannot absorb the whole debt`() {
        val payments =
            SettlementCalculator.settle(
                mapOf(
                    1L to BigDecimal("-300"),
                    2L to BigDecimal("200"),
                    3L to BigDecimal("100"),
                ),
            )

        assertEquals(
            listOf(
                payment(from = 1L, to = 2L, amount = "200"),
                payment(from = 1L, to = 3L, amount = "100"),
            ),
            payments,
        )
    }

    @Test
    fun `every creditor receives exactly its positive balance and every debtor pays exactly its debt`() {
        val balances =
            mapOf(
                1L to BigDecimal("600.5000"),
                2L to BigDecimal("399.5000"),
                3L to BigDecimal("-500.2500"),
                4L to BigDecimal("-499.7500"),
            )

        val payments = SettlementCalculator.settle(balances)

        assertEquals(3, payments.size)
        balances.forEach { (memberId, balance) ->
            if (balance.signum() > 0) {
                assertEquals(0, receivedBy(memberId, payments).compareTo(balance))
                assertEquals(0, paidBy(memberId, payments).signum())
            } else {
                assertEquals(0, paidBy(memberId, payments).compareTo(balance.negate()))
                assertEquals(0, receivedBy(memberId, payments).signum())
            }
        }
    }

    @Test
    fun `a split already settled suggests no payments`() {
        val payments =
            SettlementCalculator.settle(
                mapOf(
                    1L to BigDecimal("0.0000"),
                    2L to BigDecimal.ZERO,
                ),
            )

        assertTrue(payments.isEmpty())
    }

    @Test
    fun `an event without members suggests no payments`() {
        assertTrue(SettlementCalculator.settle(emptyMap()).isEmpty())
    }

    @Test
    fun `a rounding residue below one cent is considered settled`() {
        val payments =
            SettlementCalculator.settle(
                mapOf(
                    1L to BigDecimal("66.6667"),
                    2L to BigDecimal("-33.3333"),
                    3L to BigDecimal("-33.3333"),
                ),
            )

        assertEquals(
            listOf(
                payment(from = 2L, to = 1L, amount = "33.3333"),
                payment(from = 3L, to = 1L, amount = "33.3333"),
            ),
            payments,
        )
    }
}
