package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.domain.service.UserService
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.hibernate.exception.ConstraintViolationException
import org.hibernate.exception.ConstraintViolationException.ConstraintKind
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SignUpServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val userService = UserService()
    private val signUpService = SignUpService(userRepository, passwordEncoder, userService)

    @Test
    fun `register saves one user with normalized email and hashed password`() {
        whenever(userRepository.existsByEmail("ada@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("una-clave-segura")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            val unsaved = invocation.getArgument<User>(0)
            User(
                id = 1L,
                name = unsaved.name,
                email = unsaved.email,
                passwordHash = unsaved.passwordHash,
            )
        }

        val result =
            signUpService.register(
                name = "Ada Lovelace",
                email = "  Ada@Example.com  ",
                password = "una-clave-segura",
            )

        val saved = argumentCaptor<User>()
        verify(userRepository).save(saved.capture())
        verify(passwordEncoder).encode("una-clave-segura")

        assertEquals(1L, result.id)
        assertEquals("Ada Lovelace", result.name)
        assertEquals("ada@example.com", result.email)
        assertEquals("hashed", result.passwordHash)
        assertEquals("ada@example.com", saved.firstValue.email)
        assertEquals("hashed", saved.firstValue.passwordHash)
        assertNotEquals("una-clave-segura", saved.firstValue.passwordHash)
    }

    @Test
    fun `register rejects mixed-case email that already exists`() {
        whenever(userRepository.existsByEmail("ada@example.com")).thenReturn(true)

        assertFailsWith<EmailAlreadyInUseException> {
            signUpService.register(
                name = "Ada Lovelace",
                email = "Ada@Example.com",
                password = "una-clave-segura",
            )
        }

        verify(userRepository, never()).save(any())
        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `register maps unique constraint violation to email already in use`() {
        whenever(userRepository.existsByEmail("ada@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("una-clave-segura")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenThrow(duplicateEmailViolation())

        assertFailsWith<EmailAlreadyInUseException> {
            signUpService.register(
                name = "Ada Lovelace",
                email = "ada@example.com",
                password = "una-clave-segura",
            )
        }
    }

    @Test
    fun `register does not map other integrity violations to email already in use`() {
        whenever(userRepository.existsByEmail("ada@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("una-clave-segura")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenThrow(columnTooLongViolation())

        assertFailsWith<DataIntegrityViolationException> {
            signUpService.register(
                name = "Ada Lovelace",
                email = "ada@example.com",
                password = "una-clave-segura",
            )
        }
    }

    private fun duplicateEmailViolation(): DataIntegrityViolationException {
        val cause =
            ConstraintViolationException(
                "duplicate email",
                SQLException("duplicate key"),
                ConstraintKind.UNIQUE,
                "uk_users_email",
            )
        return DataIntegrityViolationException("duplicate email", cause)
    }

    private fun columnTooLongViolation(): DataIntegrityViolationException {
        val cause =
            ConstraintViolationException(
                "value too long",
                SQLException("value too long"),
                ConstraintKind.OTHER,
                null as String?,
            )
        return DataIntegrityViolationException("value too long", cause)
    }
}
