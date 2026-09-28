package edu.austral.splitit.server.infrastructure.api.event

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

class CreateExpenseRequestValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `valid request produces no violations`() {
        val request =
            CreateExpenseRequest(
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "ARS",
                paidByMemberId = 1L,
            )

        val violations = validator.validate(request)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `blank name violates name property`() {
        val request =
            CreateExpenseRequest(
                name = "   ",
                amount = BigDecimal("5000"),
                currency = "ARS",
                paidByMemberId = 1L,
            )

        assertTrue(validator.validate(request).any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `amount equal to zero violates amount`() {
        val request =
            CreateExpenseRequest(
                name = "Cena",
                amount = BigDecimal.ZERO,
                currency = "ARS",
                paidByMemberId = 1L,
            )

        assertTrue(validator.validate(request).any { it.propertyPath.toString() == "amount" })
    }

    @Test
    fun `negative amount violates amount`() {
        val request =
            CreateExpenseRequest(
                name = "Cena",
                amount = BigDecimal("-1"),
                currency = "ARS",
                paidByMemberId = 1L,
            )

        assertTrue(validator.validate(request).any { it.propertyPath.toString() == "amount" })
    }

    @Test
    fun `currency with length different from 3 violates currency`() {
        val requestShort =
            CreateExpenseRequest(
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "AR",
                paidByMemberId = 1L,
            )
        val requestLong =
            CreateExpenseRequest(
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "PESOS",
                paidByMemberId = 1L,
            )

        assertTrue(validator.validate(requestShort).any { it.propertyPath.toString() == "currency" })
        assertTrue(validator.validate(requestLong).any { it.propertyPath.toString() == "currency" })
    }

    @Test
    fun `non letter currency violates currency`() {
        val request =
            CreateExpenseRequest(
                name = "Cena",
                amount = BigDecimal("5000"),
                currency = "123",
                paidByMemberId = 1L,
            )

        assertTrue(validator.validate(request).any { it.propertyPath.toString() == "currency" })
    }
}
