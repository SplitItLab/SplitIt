package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LoginServiceTest {
    private val userService: UserService = mock()
    private val passwordEncoder: PasswordEncoder = mock()

    private val loginService = LoginService(passwordEncoder, userService)

    @Test
    fun `login returns the user when email is normalized and the password matches`() {
        whenever(
            userService.findByEmail(
                Helpers.emailOf("ada@example.com"),
            ),
        ).thenReturn(ada())

        whenever(passwordEncoder.matches("una-clave-segura", "hashed")).thenReturn(true)

        val result = loginService.login(Helpers.emailOf("  Ada@Example.com  "), "una-clave-segura")

        assertEquals(1L, result.id)
        assertEquals("Ada Lovelace", result.name)
        assertEquals("ada@example.com", result.email)
        verify(passwordEncoder).matches("una-clave-segura", "hashed")
    }

    @Test
    fun `unknown email and wrong password raise the same exception`() {
        whenever(userService.findByEmail(Helpers.emailOf("missing@example.com"))).thenReturn(null)
        whenever(userService.findByEmail(Helpers.emailOf("ada@example.com"))).thenReturn(ada())
        whenever(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false)

        val unknownEmail =
            assertFailsWith<InvalidCredentialsException> {
                loginService.login(Helpers.emailOf("missing@example.com"), "una-clave-segura")
            }
        val wrongPassword =
            assertFailsWith<InvalidCredentialsException> {
                loginService.login(Helpers.emailOf("ada@example.com"), "wrong-password")
            }

        assertEquals(unknownEmail.message, wrongPassword.message)
        assertEquals("Invalid credentials", unknownEmail.message)
        verify(passwordEncoder).matches(eq("una-clave-segura"), any())
        verify(passwordEncoder).matches(eq("wrong-password"), eq("hashed"))
    }

    @Test
    fun `unknown email still verifies the password against a dummy hash`() {
        whenever(userService.findByEmail(Helpers.emailOf("missing@example.com"))).thenReturn(null)
        whenever(passwordEncoder.matches(eq("una-clave-segura"), any())).thenReturn(true)

        assertFailsWith<InvalidCredentialsException> {
            loginService.login(Helpers.emailOf("missing@example.com"), "una-clave-segura")
        }

        verify(passwordEncoder).matches(eq("una-clave-segura"), any())
    }

    private fun ada(): User =
        User(
            id = 1L,
            name = "Ada Lovelace",
            email = "ada@example.com",
            passwordHash = "hashed",
        )
}
