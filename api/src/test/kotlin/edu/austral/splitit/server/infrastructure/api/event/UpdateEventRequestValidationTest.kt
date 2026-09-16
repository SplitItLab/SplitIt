package edu.austral.splitit.server.infrastructure.api.event

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class UpdateEventRequestValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `valid request produces no violations`() {
        val request =
            UpdateEventRequest(
                name = "Viaje a Mendoza",
                description = "Gastos del fin de semana",
                iconKey = "car",
            )

        val violations = validator.validate(request)
        assert(violations.isEmpty())
    }

    @Test
    fun `blank name violates name property`() {
        val request =
            UpdateEventRequest(
                name = "   ",
                description = "Gastos del fin de semana",
                iconKey = "car",
            )

        val violations = validator.validate(request)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `icon key too long`() {
        val request =
            UpdateEventRequest(
                name = "Viaje a Mendoza",
                description = "Gastos del fin de semana",
                iconKey = "car".repeat(101),
            )

        val violations = validator.validate(request)
        assertTrue(violations.any { it.propertyPath.toString() == "iconKey" })
    }
}
