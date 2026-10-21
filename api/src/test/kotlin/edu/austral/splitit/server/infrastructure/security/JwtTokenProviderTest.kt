package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.AuthUser
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtTokenProviderTest {
    private val provider = JwtTokenProvider(SECRET, EXPIRATION_HOURS, Clock.systemUTC())

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
        val parts = issued.split('.')
        // Signing input is "header.payload". Changing leftover Base64URL bits on the
        // last signature character can leave the decoded HMAC unchanged (~1/16).
        val tampered = "${parts[0]}.${parts[1]}x.${parts[2]}"

        assertNotEquals(issued, tampered)
        assertNull(provider.parse(tampered))
    }

    @Test
    fun `parse returns null for an expired token`() {
        val expiredProvider = JwtTokenProvider(SECRET, EXPIRED_HOURS, Clock.systemUTC())
        val issued = expiredProvider.issue(adaAuthUser())

        assertNull(expiredProvider.parse(issued))
    }

    @Test
    fun `parseGuest returns the issued guest`() {
        val issued = provider.issueGuest(juanGuest())

        assertEquals(juanGuest(), provider.parseGuest(issued))
    }

    @Test
    fun `a guest token is not a user token and a user token is not a guest token`() {
        val guestToken = provider.issueGuest(juanGuest())
        val userToken = provider.issue(adaAuthUser())

        assertNull(provider.parse(guestToken))
        assertNull(provider.parseGuest(userToken))
    }

    @Test
    fun `a guest token expires after seven days`() {
        val issuedAt = Instant.parse("2026-01-01T12:00:00Z")
        val issued = providerAt(issuedAt).issueGuest(juanGuest())
        val sevenDaysLater = issuedAt.plus(Duration.ofDays(7))

        assertNotNull(providerAt(sevenDaysLater.minusSeconds(60)).parseGuest(issued))
        assertNull(providerAt(sevenDaysLater.plusSeconds(60)).parseGuest(issued))
    }

    private fun providerAt(instant: Instant): JwtTokenProvider =
        JwtTokenProvider(SECRET, EXPIRATION_HOURS, Clock.fixed(instant, ZoneOffset.UTC))

    private fun adaAuthUser(): AuthUser =
        AuthUser(
            id = 1L,
            username = "ada@example.com",
            password = "",
            roles = emptyList(),
            name = "Ada Lovelace",
        )

    private fun juanGuest(): AuthGuest = AuthGuest(eventId = 10L, memberId = 30L, name = "Juan")

    private companion object {
        const val SECRET = "test-jwt-secret-that-is-long-enough"
        const val EXPIRATION_HOURS = 8L
        const val EXPIRED_HOURS = -1L
    }
}
