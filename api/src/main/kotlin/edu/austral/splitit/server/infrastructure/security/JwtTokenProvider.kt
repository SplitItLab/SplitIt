package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

const val MIN_JWT_SECRET_LENGTH = 32

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-hours}") private val expirationHours: Long,
) : TokenProvider {
    private val key: SecretKey

    init {
        require(secret.length >= MIN_JWT_SECRET_LENGTH) {
            "JWT_SECRET must be at least $MIN_JWT_SECRET_LENGTH characters"
        }
        key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    override fun issue(user: AuthUser): String {
        val now = Instant.now()
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
        try {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            val username = claims[USERNAME_CLAIM] as? String ?: return null
            val name = claims[NAME_CLAIM] as? String ?: return null
            val roles = rolesFromClaim(claims[ROLES_CLAIM]) ?: return null
            AuthUser(
                id = claims.subject.toLong(),
                username = username,
                password = "",
                roles = roles,
                name = name,
            )
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

    companion object {
        private const val USERNAME_CLAIM = "username"
        private const val NAME_CLAIM = "name"
        private const val ROLES_CLAIM = "roles"
    }
}
