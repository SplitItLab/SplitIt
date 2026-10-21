package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

const val MIN_JWT_SECRET_LENGTH = 32

val GUEST_SESSION_TTL: Duration = Duration.ofDays(7)

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-hours}") private val expirationHours: Long,
    private val clock: Clock,
) : TokenProvider {
    private val key: SecretKey
    private val guestKey: SecretKey

    init {
        require(secret.length >= MIN_JWT_SECRET_LENGTH) {
            "JWT_SECRET must be at least $MIN_JWT_SECRET_LENGTH characters"
        }
        val secretBytes = secret.toByteArray(StandardCharsets.UTF_8)
        key = Keys.hmacShaKeyFor(secretBytes)
        // A separate key makes a guest token unverifiable as a user token, and vice versa.
        guestKey = Keys.hmacShaKeyFor(deriveKey(secretBytes, GUEST_KEY_CONTEXT))
    }

    override fun issue(user: AuthUser): String {
        val now = clock.instant()
        return Jwts
            .builder()
            .subject(user.id.toString())
            .claim(USERNAME_CLAIM, user.username)
            .claim(NAME_CLAIM, user.name)
            .claim(ROLES_CLAIM, user.roles.toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
            .signWith(key)
            .compact()
    }

    override fun parse(token: String): AuthUser? =
        parseClaims(token, key)?.let { claims ->
            val username = claims[USERNAME_CLAIM] as? String ?: return null
            val name = claims[NAME_CLAIM] as? String ?: return null
            val roles = rolesFromClaim(claims[ROLES_CLAIM]) ?: return null
            AuthUser(
                id = claims.subject?.toLongOrNull() ?: return null,
                username = username,
                password = "",
                roles = roles,
                name = name,
            )
        }

    override fun issueGuest(guest: AuthGuest): String {
        val now = clock.instant()
        return Jwts
            .builder()
            .subject(guest.memberId.toString())
            .claim(EVENT_ID_CLAIM, guest.eventId)
            .claim(NAME_CLAIM, guest.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(GUEST_SESSION_TTL)))
            .signWith(guestKey)
            .compact()
    }

    override fun parseGuest(token: String): AuthGuest? =
        parseClaims(token, guestKey)?.let { claims ->
            val eventId = (claims[EVENT_ID_CLAIM] as? Number)?.toLong() ?: return null
            val name = claims[NAME_CLAIM] as? String ?: return null
            AuthGuest(
                eventId = eventId,
                memberId = claims.subject?.toLongOrNull() ?: return null,
                name = name,
            )
        }

    private fun parseClaims(
        token: String,
        verificationKey: SecretKey,
    ): Claims? =
        try {
            Jwts
                .parser()
                .verifyWith(verificationKey)
                .clock { Date.from(clock.instant()) }
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: JwtException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun rolesFromClaim(raw: Any?): List<String>? =
        when (raw) {
            is List<*> -> {
                val roles = raw.filterIsInstance<String>()
                roles.takeIf { it.size == raw.size }
            }
            is String -> listOf(raw)
            else -> null
        }

    private fun deriveKey(
        secret: ByteArray,
        context: String,
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(secret, HMAC_SHA_256))
        return mac.doFinal(context.toByteArray(StandardCharsets.UTF_8))
    }

    companion object {
        private const val USERNAME_CLAIM = "username"
        private const val NAME_CLAIM = "name"
        private const val ROLES_CLAIM = "roles"
        private const val EVENT_ID_CLAIM = "eventId"
        private const val GUEST_KEY_CONTEXT = "splitit-guest-token"
        private const val HMAC_SHA_256 = "HmacSHA256"
    }
}
