package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.UserNotFoundException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountServiceTest {
    private val userService: UserService = mock()
    private val accountService = AccountService(userService)

    @Test
    fun `get returns the persisted account`() {
        whenever(userService.findById(1L)).thenReturn(ada())

        val user = accountService.get(1L)

        assertEquals(1L, user.id)
        assertEquals("Ada Lovelace", user.name)
        assertEquals("ada@example.com", user.email)
    }

    @Test
    fun `get raises when the account no longer exists`() {
        whenever(userService.findById(1L)).thenReturn(null)

        assertFailsWith<UserNotFoundException> {
            accountService.get(1L)
        }
    }

    @Test
    fun `update persists the new name and email`() {
        whenever(
            userService.update(
                id = 1L,
                name = "Ada Byron Lovelace",
                email = "ada.lovelace@example.com",
            ),
        ).thenReturn(
            User(
                id = 1L,
                name = "Ada Byron Lovelace",
                email = "ada.lovelace@example.com",
                passwordHash = "hashed",
            ),
        )

        val user =
            accountService.update(
                userId = 1L,
                name = "Ada Byron Lovelace",
                email = "ada.lovelace@example.com",
            )

        assertEquals("Ada Byron Lovelace", user.name)
        assertEquals("ada.lovelace@example.com", user.email)
    }

    private fun ada(): User =
        User(
            id = 1L,
            name = "Ada Lovelace",
            email = "ada@example.com",
            passwordHash = "hashed",
        )
}
