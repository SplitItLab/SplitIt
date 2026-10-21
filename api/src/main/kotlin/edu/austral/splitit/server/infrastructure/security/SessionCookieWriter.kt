package edu.austral.splitit.server.infrastructure.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

const val GUEST_COOKIE_NAME = "guest_token"

@Component
class SessionCookieWriter(
    @Value("\${auth.cookie.name}") private val cookieName: String,
    @Value("\${auth.cookie.secure}") private val secure: Boolean,
    @Value("\${auth.cookie.same-site}") private val sameSite: String,
    @Value("\${jwt.expiration-hours}") private val expirationHours: Long,
) {
    fun write(
        response: HttpServletResponse,
        token: String,
    ) {
        addCookie(response, cookieName, token, Duration.ofHours(expirationHours))
    }

    fun writeGuest(
        response: HttpServletResponse,
        token: String,
    ) {
        addCookie(response, GUEST_COOKIE_NAME, token, GUEST_SESSION_TTL)
    }

    fun clear(response: HttpServletResponse) {
        addCookie(response, cookieName, "", Duration.ZERO)
        addCookie(response, GUEST_COOKIE_NAME, "", Duration.ZERO)
    }

    private fun addCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        maxAge: Duration,
    ) {
        val cookie =
            ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
