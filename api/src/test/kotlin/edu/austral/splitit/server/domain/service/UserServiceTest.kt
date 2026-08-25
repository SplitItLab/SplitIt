package edu.austral.splitit.server.domain.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {
    private val userService = UserService()

    @Test
    fun `create trims name and email before persisting`() {
        val user =
            userService.create(
                name = "  Ada Lovelace  ",
                email = "  Ada@Example.com  ",
                passwordHash = "hashed",
            )

        assertEquals("Ada Lovelace", user.name)
        assertEquals("ada@example.com", user.email)
        assertEquals("hashed", user.passwordHash)
    }

    @Test
    fun `create rejects name that is too short after trim`() {
        assertFailsWith<IllegalArgumentException> {
            userService.create(
                name = " A ",
                email = "ada@example.com",
                passwordHash = "hashed",
            )
        }
    }
}
