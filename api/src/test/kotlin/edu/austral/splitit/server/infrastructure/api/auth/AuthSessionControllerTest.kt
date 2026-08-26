package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [
        UserDetailsServiceAutoConfiguration::class,
    ],
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "jwt.expiration-hours=8",
        "jwt.secret=test-jwt-secret-that-is-long-enough",
    ],
)
class AuthSessionControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var signUpService: SignUpService

    @MockitoBean
    private lateinit var loginService: LoginService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var sessionCookieWriter: SessionCookieWriter

    @Test
    fun `session returns the public user when the cookie is valid`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())

        mockMvc
            .perform(get("/api/auth/session").cookie(Cookie("auth_token", "good-token")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    @Test
    fun `session returns 401 without a session cookie`() {
        mockMvc
            .perform(get("/api/auth/session"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))
    }

    @Test
    fun `session returns 401 for a tampered cookie without leaking internals`() {
        whenever(tokenProvider.parse("tampered")).thenReturn(null)

        mockMvc
            .perform(get("/api/auth/session").cookie(Cookie("auth_token", "tampered")))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))
    }

    @Test
    fun `a private endpoint rejects requests without a session`() {
        mockMvc
            .perform(get("/api/private"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))
    }

    @Test
    fun `session accepts a bearer token as a fallback for direct requests`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())

        mockMvc
            .perform(
                get("/api/auth/session")
                    .header("Authorization", "Bearer good-token")
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("ada@example.com"))
    }

    @Test
    fun `session accepts a bearer token when the session cookie is stale`() {
        whenever(tokenProvider.parse("tampered")).thenReturn(null)
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())

        mockMvc
            .perform(
                get("/api/auth/session")
                    .cookie(Cookie("auth_token", "tampered"))
                    .header("Authorization", "Bearer good-token")
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("ada@example.com"))
    }

    private fun adaAuthUser(): AuthUser =
        AuthUser(
            id = 1L,
            username = "ada@example.com",
            password = "",
            roles = emptyList(),
            name = "Ada Lovelace",
        )
}
