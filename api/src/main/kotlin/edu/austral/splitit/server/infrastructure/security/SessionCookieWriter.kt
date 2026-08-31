package edu.austral.splitit.server.infrastructure.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

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
        addCookie(response, token, Duration.ofHours(expirationHours))
    }

    fun clear(response: HttpServletResponse) {
        addCookie(response, "", Duration.ZERO)
    }

    private fun addCookie(
        response: HttpServletResponse,
        value: String,
        maxAge: Duration,
    ) {
        val cookie =
            ResponseCookie
                .from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
