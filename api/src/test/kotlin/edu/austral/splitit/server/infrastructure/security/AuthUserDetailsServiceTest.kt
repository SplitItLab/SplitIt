package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthUserDetailsServiceTest {
    private val userRepository: UserRepository = mock()
    private val service = AuthUserDetailsService(userRepository)

    @Test
    fun `loads the user by normalized email`() {
        whenever(userRepository.findByEmail("ada@example.com")).thenReturn(
            User(
                id = 1L,
                name = "Ada Lovelace",
                email = "ada@example.com",
                passwordHash = "hashed",
            ),
        )

        val details = service.loadUserByUsername("  Ada@Example.com  ")

        assertEquals("ada@example.com", details.username)
        assertEquals("hashed", details.password)
    }

    @Test
    fun `unknown email raises UsernameNotFoundException`() {
        whenever(userRepository.findByEmail("missing@example.com")).thenReturn(null)

        assertFailsWith<UsernameNotFoundException> {
            service.loadUserByUsername("missing@example.com")
        }
    }
}
