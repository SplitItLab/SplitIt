package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.domain.model.event.EventDescription.Companion.EVENT_DESCRIPTION_MAX
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
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `omitted name produces no violations`() {
        val request =
            UpdateEventRequest(
                name = null,
                description = "Solo cambio de descripción",
                iconKey = null,
            )

        val violations = validator.validate(request)
        assertTrue(violations.isEmpty())
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
    fun `description longer than the max violates description`() {
        val request =
            UpdateEventRequest(
                name = null,
                description = "X".repeat(EVENT_DESCRIPTION_MAX + 1),
                iconKey = null,
            )

        assertTrue(validator.validate(request).any { it.propertyPath.toString() == "description" })
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
