package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.application.exception.UserNotFoundException
import edu.austral.splitit.server.domain.model.user.User
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
import java.sql.SQLException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UserServiceTest {
    private val userRepository: UserRepository = mock()
    private val userService = UserService(userRepository)

    @Test
    fun `save trims name and email before persisting`() {
        whenever(userRepository.saveAndFlush(any())).thenAnswer { invocation ->
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
                email = Helpers.emailOf("  Ada@Example.com  "),
                passwordHash = "hashed",
            )

        val saved = argumentCaptor<User>()
        verify(userRepository).saveAndFlush(saved.capture())

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
                email = Helpers.emailOf("ada@example.com"),
                passwordHash = "hashed",
            )
        }
    }

    @Test
    fun `save maps unique constraint violation to email already in use`() {
        whenever(userRepository.saveAndFlush(any())).thenThrow(duplicateEmailViolation())

        assertFailsWith<EmailAlreadyInUseException> {
            userService.save(
                name = "Ada Lovelace",
                email = Helpers.emailOf("ada@example.com"),
                passwordHash = "hashed",
            )
        }
    }

    @Test
    fun `save does not map other integrity violations to email already in use`() {
        whenever(userRepository.saveAndFlush(any())).thenThrow(columnTooLongViolation())

        assertFailsWith<DataIntegrityViolationException> {
            userService.save(
                name = "Ada Lovelace",
                email = Helpers.emailOf("ada@example.com"),
                passwordHash = "hashed",
            )
        }
    }

    @Test
    fun `save does not map unique violation without constraint name to email already in use`() {
        whenever(userRepository.saveAndFlush(any())).thenThrow(uniqueViolationWithoutName())

        assertFailsWith<DataIntegrityViolationException> {
            userService.save(
                name = "Ada Lovelace",
                email = Helpers.emailOf("ada@example.com"),
                passwordHash = "hashed",
            )
        }
    }

    @Test
    fun `findById returns the persisted user`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(ada()))

        val user = userService.findById(1L)

        assertEquals(1L, user?.id)
        assertEquals("Ada Lovelace", user?.name)
        assertEquals("ada@example.com", user?.email)
    }

    @Test
    fun `findById returns null when the user does not exist`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertNull(userService.findById(99L))
    }

    @Test
    fun `update saves trimmed name and normalized email together`() {
        val stored = ada()
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(stored))
        whenever(userRepository.existsByEmailAndIdNot("ada.lovelace@example.com", 1L)).thenReturn(false)
        whenever(userRepository.saveAndFlush(any())).thenAnswer { it.getArgument(0) }

        val user =
            userService.update(
                id = 1L,
                name = "  Ada Byron Lovelace  ",
                email = Helpers.emailOf("  Ada.Lovelace@Example.com  "),
            )

        val saved = argumentCaptor<User>()
        verify(userRepository).saveAndFlush(saved.capture())

        assertEquals(1L, user.id)
        assertEquals("Ada Byron Lovelace", user.name)
        assertEquals("ada.lovelace@example.com", user.email)
        assertEquals("hashed", user.passwordHash)
        assertEquals("Ada Byron Lovelace", saved.firstValue.name)
        assertEquals("ada.lovelace@example.com", saved.firstValue.email)
        assertEquals(stored, saved.firstValue)
    }

    @Test
    fun `update keeps the same email when only the name changes`() {
        val stored = ada()
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(stored))
        whenever(userRepository.existsByEmailAndIdNot("ada@example.com", 1L)).thenReturn(false)
        whenever(userRepository.saveAndFlush(any())).thenAnswer { it.getArgument(0) }

        val user =
            userService.update(
                id = 1L,
                name = "Ada Byron Lovelace",
                email = Helpers.emailOf("Ada@Example.com"),
            )

        assertEquals("Ada Byron Lovelace", user.name)
        assertEquals("ada@example.com", user.email)
        verify(userRepository).saveAndFlush(stored)
    }

    @Test
    fun `update rejects an email already used by another account`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(ada()))
        whenever(userRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true)

        assertFailsWith<EmailAlreadyInUseException> {
            userService.update(
                id = 1L,
                name = "Ada Lovelace",
                email = Helpers.emailOf("taken@example.com"),
            )
        }

        verify(userRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `update maps unique constraint violation to email already in use`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(ada()))
        whenever(userRepository.existsByEmailAndIdNot("ada.lovelace@example.com", 1L)).thenReturn(false)
        whenever(userRepository.saveAndFlush(any())).thenThrow(duplicateEmailViolation())

        assertFailsWith<EmailAlreadyInUseException> {
            userService.update(
                id = 1L,
                name = "Ada Byron Lovelace",
                email = Helpers.emailOf("ada.lovelace@example.com"),
            )
        }
    }

    @Test
    fun `update rejects a missing account`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            userService.update(
                id = 99L,
                name = "Ada Lovelace",
                email = Helpers.emailOf("ada@example.com"),
            )
        }

        verify(userRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `update rejects a name that is too short after trim`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(ada()))
        whenever(userRepository.existsByEmailAndIdNot("ada@example.com", 1L)).thenReturn(false)

        assertFailsWith<IllegalArgumentException> {
            userService.update(
                id = 1L,
                name = " A ",
                email = Helpers.emailOf("ada@example.com"),
            )
        }

        verify(userRepository, never()).saveAndFlush(any())
    }

    private fun ada(): User =
        User(
            id = 1L,
            name = "Ada Lovelace",
            email = "ada@example.com",
            passwordHash = "hashed",
        )

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

    private fun uniqueViolationWithoutName(): DataIntegrityViolationException {
        val cause =
            ConstraintViolationException(
                "duplicate email",
                SQLException("duplicate key"),
                ConstraintKind.UNIQUE,
                null as String?,
            )
        return DataIntegrityViolationException("duplicate email", cause)
    }
}
