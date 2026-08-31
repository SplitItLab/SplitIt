package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@Import(GlobalExceptionHandler::class, SessionCookieWriter::class)
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "jwt.expiration-hours=8",
        "cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000",
    ],
)
class LogoutControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var signUpService: SignUpService

    @MockitoBean
    private lateinit var loginService: LoginService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `logout returns 204 and expires the session cookie`() {
        mockMvc
            .perform(post("/api/auth/logout").cookie(Cookie("auth_token", "signed.jwt.token")))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, expiredCookie()))

        verifyNoAccountChanges()
    }

    @Test
    fun `logout returns 204 without a session cookie`() {
        mockMvc
            .perform(post("/api/auth/logout"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, expiredCookie()))

        verifyNoAccountChanges()
    }

    @Test
    fun `logout is idempotent`() {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent)
        mockMvc
            .perform(post("/api/auth/logout"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, expiredCookie()))

        verifyNoAccountChanges()
    }

    @Test
    fun `logout from an allowed origin returns 204 and expires the cookie`() {
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .cookie(Cookie("auth_token", "signed.jwt.token")),
            ).andExpect(status().isNoContent)
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, expiredCookie()))

        verifyNoAccountChanges()
    }

    @Test
    fun `logout from a cross-site origin returns 403 without clearing the cookie`() {
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header(HttpHeaders.ORIGIN, "https://evil.example")
                    .cookie(Cookie("auth_token", "signed.jwt.token")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))

        verifyNoAccountChanges()
    }

    @Test
    fun `logout from a cross-site referer returns 403 without clearing the cookie`() {
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header(HttpHeaders.REFERER, "https://evil.example/attack")
                    .cookie(Cookie("auth_token", "signed.jwt.token")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))

        verifyNoAccountChanges()
    }

    private fun expiredCookie() =
        allOf(
            containsString("auth_token=;"),
            containsString("HttpOnly"),
            containsString("Path=/"),
            containsString("SameSite=Lax"),
            containsString("Max-Age=0"),
            not(containsString("Secure")),
            not(containsString("password")),
        )

    private fun verifyNoAccountChanges() {
        verify(loginService, never()).login(any(), any())
        verify(signUpService, never()).register(any(), any(), any())
        verify(tokenProvider, never()).issue(any())
    }
}
