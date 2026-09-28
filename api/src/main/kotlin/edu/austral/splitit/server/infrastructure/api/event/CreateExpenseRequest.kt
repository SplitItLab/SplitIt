package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.domain.model.event.Currency.Companion.CURRENCY_LENGTH
import edu.austral.splitit.server.domain.model.event.Currency.Companion.VALID_REGEX
import edu.austral.splitit.server.domain.model.event.Expense.Companion.EXPENSE_NAME_MAX
import edu.austral.splitit.server.domain.model.event.Expense.Companion.EXPENSE_NAME_MIN
import edu.austral.splitit.server.infrastructure.api.auth.TrimmedSize
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

data class CreateExpenseRequest(
    @field:NotBlank
    @field:TrimmedSize(min = EXPENSE_NAME_MIN, max = EXPENSE_NAME_MAX, allowSurroundingWhitespace = true)
    val name: String,
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = false)
    val amount: BigDecimal,
    @field:NotBlank
    @field:TrimmedSize(min = CURRENCY_LENGTH, max = CURRENCY_LENGTH)
    @field:Pattern(regexp = VALID_REGEX)
    val currency: String,
    @field:NotNull
    val paidByMemberId: Long,
)
