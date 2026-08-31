package edu.austral.splitit.server.infrastructure.security

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletResponse

class SessionCookieWriterTest {
    @Test
    fun `write sets an HttpOnly session cookie with local attributes`() {
        val response = MockHttpServletResponse()

        localWriter().write(response, TOKEN)

        assertThat(
            setCookie(response),
            allOf(
                containsString("auth_token=$TOKEN"),
                containsString("HttpOnly"),
                containsString("Path=/"),
                containsString("SameSite=Lax"),
                containsString("Max-Age=28800"),
                not(containsString("Secure")),
            ),
        )
    }

    @Test
    fun `clear expires the same cookie attributes used by write`() {
        val response = MockHttpServletResponse()

        localWriter().clear(response)

        assertThat(
            setCookie(response),
            allOf(
                containsString("auth_token=;"),
                containsString("HttpOnly"),
                containsString("Path=/"),
                containsString("SameSite=Lax"),
                containsString("Max-Age=0"),
                not(containsString("Secure")),
                not(containsString(TOKEN)),
            ),
        )
    }

    @Test
    fun `write and clear keep Secure when production cookies require it`() {
        val writer =
            SessionCookieWriter(
                cookieName = COOKIE_NAME,
                secure = true,
                sameSite = SAME_SITE,
                expirationHours = EXPIRATION_HOURS,
            )
        val written = MockHttpServletResponse()
        val cleared = MockHttpServletResponse()

        writer.write(written, TOKEN)
        writer.clear(cleared)

        assertThat(
            setCookie(written),
            allOf(
                containsString("auth_token=$TOKEN"),
                containsString("HttpOnly"),
                containsString("Path=/"),
                containsString("SameSite=Lax"),
                containsString("Max-Age=28800"),
                containsString("Secure"),
            ),
        )
        assertThat(
            setCookie(cleared),
            allOf(
                containsString("auth_token=;"),
                containsString("HttpOnly"),
                containsString("Path=/"),
                containsString("SameSite=Lax"),
                containsString("Max-Age=0"),
                containsString("Secure"),
                not(containsString(TOKEN)),
            ),
        )
    }

    private fun localWriter(): SessionCookieWriter =
        SessionCookieWriter(
            cookieName = COOKIE_NAME,
            secure = false,
            sameSite = SAME_SITE,
            expirationHours = EXPIRATION_HOURS,
        )

    private fun setCookie(response: MockHttpServletResponse): String {
        val header = response.getHeader(HttpHeaders.SET_COOKIE)
        return requireNotNull(header)
    }

    private companion object {
        const val COOKIE_NAME = "auth_token"
        const val SAME_SITE = "Lax"
        const val EXPIRATION_HOURS = 8L
        const val TOKEN = "signed.jwt.token"
    }
}
