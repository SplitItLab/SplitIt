package edu.austral.splitit.server.infrastructure.api.auth

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class SignUpRequestValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `name that is too short after trim violates the name property`() {
        val violations =
            validator.validate(
                SignUpRequest(
                    name = " A ",
                    email = "ada@example.com",
                    password = "una-clave-segura",
                ),
            )

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `padded name with valid trimmed length violates the name property`() {
        val violations =
            validator.validate(
                SignUpRequest(
                    name = " Ada Lovelace ",
                    email = "ada@example.com",
                    password = "una-clave-segura",
                ),
            )

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }
}
