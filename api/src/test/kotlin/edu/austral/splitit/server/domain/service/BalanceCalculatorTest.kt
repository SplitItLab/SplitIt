package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.Expense
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class BalanceCalculatorTest {
    private val owner = Helpers.user(name = "Ana", email = "ana@example.com")
    private val event =
        Event
            .create(owner = owner, name = "Asado", baseCurrency = "ARS")
            .apply { id = 10L }
    private val ana = EventMember.create(event, "Ana", owner).apply { id = 1L }
    private val luis = EventMember.create(event, "Luis").apply { id = 2L }

    @Test
    fun `balances are zero with scale 4 when the event has no expenses`() {
        val balances = BalanceCalculator.calculate(members = listOf(ana, luis), expenses = emptyList())

        assertEquals(2, balances.size)
        assertEquals(1L, balances[0].memberId)
        assertEquals("Ana", balances[0].memberName)
        assertEquals(BigDecimal("0.0000"), balances[0].netBalance)
        assertEquals(2L, balances[1].memberId)
        assertEquals("Luis", balances[1].memberName)
        assertEquals(BigDecimal("0.0000"), balances[1].netBalance)
    }

    @Test
    fun `the member who paid the only expense is owed half by the other member`() {
        val balances =
            BalanceCalculator.calculate(
                members = listOf(ana, luis),
                expenses = listOf(expense(paidBy = ana, amount = "100")),
            )

        assertEquals(BigDecimal("50.0000"), balances[0].netBalance)
        assertEquals(BigDecimal("-50.0000"), balances[1].netBalance)
    }

    @Test
    fun `equal shares leave the member who paid more with a positive balance`() {
        val balances =
            BalanceCalculator.calculate(
                members = listOf(ana, luis),
                expenses =
                    listOf(
                        expense(paidBy = ana, amount = "60"),
                        expense(paidBy = luis, amount = "40"),
                    ),
            )

        assertEquals(BigDecimal("10.0000"), balances[0].netBalance)
        assertEquals(BigDecimal("-10.0000"), balances[1].netBalance)
    }

    @Test
    fun `members who each paid the same share are even`() {
        val balances =
            BalanceCalculator.calculate(
                members = listOf(ana, luis),
                expenses =
                    listOf(
                        expense(paidBy = ana, amount = "50"),
                        expense(paidBy = luis, amount = "50"),
                    ),
            )

        assertEquals(BigDecimal("0.0000"), balances[0].netBalance)
        assertEquals(BigDecimal("0.0000"), balances[1].netBalance)
    }

    @Test
    fun `a member added after an expense still shares that expense`() {
        val sofia = EventMember.create(event, "Sofía").apply { id = 3L }
        val balances =
            BalanceCalculator.calculate(
                members = listOf(ana, luis, sofia),
                expenses = listOf(expense(paidBy = ana, amount = "90")),
            )

        assertEquals(BigDecimal("60.0000"), balances[0].netBalance)
        assertEquals(BigDecimal("-30.0000"), balances[1].netBalance)
        assertEquals(BigDecimal("-30.0000"), balances[2].netBalance)
        assertEquals("Sofía", balances[2].memberName)
    }

    @Test
    fun `shares round half up to four decimal places`() {
        val sofia = EventMember.create(event, "Sofía").apply { id = 3L }
        val balances =
            BalanceCalculator.calculate(
                members = listOf(ana, luis, sofia),
                expenses = listOf(expense(paidBy = ana, amount = "10")),
            )

        assertEquals(BigDecimal("6.6667"), balances[0].netBalance)
        assertEquals(BigDecimal("-3.3333"), balances[1].netBalance)
        assertEquals(BigDecimal("-3.3333"), balances[2].netBalance)
    }

    @Test
    fun `an event with no members has no balances`() {
        val balances =
            BalanceCalculator.calculate(
                members = emptyList(),
                expenses = listOf(expense(paidBy = ana, amount = "100")),
            )

        assertEquals(emptyList(), balances)
    }

    private fun expense(
        paidBy: EventMember,
        amount: String,
    ): Expense =
        Expense.create(
            event = event,
            paidByMember = paidBy,
            name = "Gasto",
            originalAmount = BigDecimal(amount),
            originalCurrency = "ARS",
            expenseDate = LocalDate.of(2024, 3, 16),
        )
}
