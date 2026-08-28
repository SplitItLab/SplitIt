package edu.austral.splitit.server.infrastructure.api.me

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class UpdateMeRequestValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `padded name with valid trimmed length is accepted`() {
        val violations =
            validator.validate(
                UpdateMeRequest(
                    name = " Ada Byron Lovelace ",
                    email = "ada.lovelace@example.com",
                ),
            )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `name that is too short after trim violates the name property`() {
        val violations =
            validator.validate(
                UpdateMeRequest(
                    name = " A ",
                    email = "ada@example.com",
                ),
            )

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `blank name violates the name property`() {
        val violations =
            validator.validate(
                UpdateMeRequest(
                    name = "   ",
                    email = "ada@example.com",
                ),
            )

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }
}
