package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.AuthUser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtTokenProviderTest {
    private val provider = JwtTokenProvider(SECRET, EXPIRATION_HOURS)

    @Test
    fun `parse returns the issued public claims`() {
        val issued = provider.issue(adaAuthUser())

        val parsed = provider.parse(issued)

        assertEquals(1L, parsed?.id)
        assertEquals("ada@example.com", parsed?.username)
        assertEquals("Ada Lovelace", parsed?.name)
        assertEquals(emptyList(), parsed?.roles)
        assertEquals("", parsed?.password)
    }

    @Test
    fun `parse returns null for a tampered token`() {
        val issued = provider.issue(adaAuthUser())

        assertNull(provider.parse(issued.dropLast(1) + "x"))
    }

    @Test
    fun `parse returns null for an expired token`() {
        val expiredProvider = JwtTokenProvider(SECRET, EXPIRED_HOURS)
        val issued = expiredProvider.issue(adaAuthUser())

        assertNull(expiredProvider.parse(issued))
    }

    private fun adaAuthUser(): AuthUser =
        AuthUser(
            id = 1L,
            username = "ada@example.com",
            password = "",
            roles = emptyList(),
            name = "Ada Lovelace",
        )

    private companion object {
        const val SECRET = "test-jwt-secret-that-is-long-enough"
        const val EXPIRATION_HOURS = 8L
        const val EXPIRED_HOURS = -1L
    }
}
