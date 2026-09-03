package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SignUpServiceTest {
    private val passwordEncoder: PasswordEncoder = mock()
    private val userService: UserService = mock()
    private val signUpService = SignUpService(passwordEncoder, userService)

    @Test
    fun `register saves one user with normalized email and hashed password`() {
        whenever(userService.findByEmail(Helpers.emailOf("ada@example.com"))).thenReturn(null)
        whenever(passwordEncoder.encode("una-clave-segura")).thenReturn("hashed")
        whenever(userService.save("Ada Lovelace", Helpers.emailOf("ada@example.com"), "hashed")).thenReturn(
            User(
                id = 1L,
                name = "Ada Lovelace",
                email = "ada@example.com",
                passwordHash = "hashed",
            ),
        )

        val result =
            signUpService.register(
                name = "Ada Lovelace",
                email = Helpers.emailOf("  Ada@Example.com  "),
                password = "una-clave-segura",
            )

        verify(passwordEncoder).encode("una-clave-segura")
        verify(userService).save("Ada Lovelace", Helpers.emailOf("ada@example.com"), "hashed")

        assertEquals(1L, result.id)
        assertEquals("Ada Lovelace", result.name)
        assertEquals("ada@example.com", result.email)
        assertEquals("hashed", result.passwordHash)
        assertNotEquals("una-clave-segura", result.passwordHash)
    }

    @Test
    fun `register rejects mixed-case email that already exists`() {
        whenever(userService.findByEmail(Helpers.emailOf("ada@example.com"))).thenReturn(
            User(
                id = 1L,
                name = "Ada Lovelace",
                email = "ada@example.com",
                passwordHash = "hashed",
            ),
        )

        assertFailsWith<EmailAlreadyInUseException> {
            signUpService.register(
                name = "Ada Lovelace",
                email = Helpers.emailOf("Ada@Example.com"),
                password = "una-clave-segura",
            )
        }

        verify(userService, never()).save(any(), any(), any())
        verify(passwordEncoder, never()).encode(any())
    }
}
