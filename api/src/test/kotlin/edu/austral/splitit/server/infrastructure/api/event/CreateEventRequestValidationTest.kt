package edu.austral.splitit.server.infrastructure.api.event

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CreateEventRequestValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `valid request produces no violations`() {
        val request =
            CreateEventRequest(
                name = "Viaje a Mendoza",
                description = "Gastos del fin de semana",
                iconKey = "car",
                baseCurrency = "ARS",
                participantNames = listOf("Ana", "Juan"),
            )

        val violations = validator.validate(request)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `blank name violates name property`() {
        val request =
            CreateEventRequest(
                name = "   ",
                baseCurrency = "ARS",
            )

        val violations = validator.validate(request)
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `currency with length different from 3 violates baseCurrency`() {
        val requestShort =
            CreateEventRequest(
                name = "Viaje",
                baseCurrency = "AR",
            )
        val requestLong =
            CreateEventRequest(
                name = "Viaje",
                baseCurrency = "PESOS",
            )

        assertTrue(validator.validate(requestShort).any { it.propertyPath.toString() == "baseCurrency" })
        assertTrue(validator.validate(requestLong).any { it.propertyPath.toString() == "baseCurrency" })
    }

    @Test
    fun `empty or blank participant name violates participantNames`() {
        val requestEmpty =
            CreateEventRequest(
                name = "Viaje",
                baseCurrency = "ARS",
                participantNames = listOf("Ana", ""),
            )
        val requestBlank =
            CreateEventRequest(
                name = "Viaje",
                baseCurrency = "ARS",
                participantNames = listOf("   "),
            )

        val violationsEmpty = validator.validate(requestEmpty)
        val violationsBlank = validator.validate(requestBlank)

        assertTrue(violationsEmpty.any { it.propertyPath.toString().startsWith("participantNames") })
        assertTrue(violationsBlank.any { it.propertyPath.toString().startsWith("participantNames") })
    }

    @Test
    fun `non letter currency violates baseCurrency`() {
        val request =
            CreateEventRequest(
                name = "Viaje",
                baseCurrency = "123",
            )

        assertTrue(validator.validate(request).any { it.propertyPath.toString() == "baseCurrency" })
    }
}
