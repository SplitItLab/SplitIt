package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.hibernate.exception.ConstraintViolationException
import org.hibernate.exception.ConstraintViolationException.ConstraintKind
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {
    private val userRepository: UserRepository = mock()
    private val userService = UserService(userRepository)

    @Test
    fun `save trims name and email before persisting`() {
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            val unsaved = invocation.getArgument<User>(0)
            User(
                id = 1L,
                name = unsaved.name,
                email = unsaved.email,
                passwordHash = unsaved.passwordHash,
            )
        }

        val user =
            userService.save(
                name = "  Ada Lovelace  ",
                email = "  Ada@Example.com  ",
                passwordHash = "hashed",
            )

        val saved = argumentCaptor<User>()
        verify(userRepository).save(saved.capture())

        assertEquals(1L, user.id)
        assertEquals("Ada Lovelace", user.name)
        assertEquals("ada@example.com", user.email)
        assertEquals("hashed", user.passwordHash)
        assertEquals("Ada Lovelace", saved.firstValue.name)
        assertEquals("ada@example.com", saved.firstValue.email)
        assertEquals("hashed", saved.firstValue.passwordHash)
    }

    @Test
    fun `save rejects name that is too short after trim`() {
        assertFailsWith<IllegalArgumentException> {
            userService.save(
                name = " A ",
                email = "ada@example.com",
                passwordHash = "hashed",
            )
        }
    }

    @Test
    fun `save maps unique constraint violation to email already in use`() {
        whenever(userRepository.save(any())).thenThrow(duplicateEmailViolation())

        assertFailsWith<EmailAlreadyInUseException> {
            userService.save(
                name = "Ada Lovelace",
                email = "ada@example.com",
                passwordHash = "hashed",
            )
        }
    }

    @Test
    fun `save does not map other integrity violations to email already in use`() {
        whenever(userRepository.save(any())).thenThrow(columnTooLongViolation())

        assertFailsWith<DataIntegrityViolationException> {
            userService.save(
                name = "Ada Lovelace",
                email = "ada@example.com",
                passwordHash = "hashed",
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
