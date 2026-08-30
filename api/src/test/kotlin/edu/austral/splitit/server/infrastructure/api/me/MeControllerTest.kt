package edu.austral.splitit.server.infrastructure.api.me

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.application.exception.UserNotFoundException
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.AccountService
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals

@WebMvcTest(
    controllers = [MeController::class],
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
class MeControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var sessionCookieWriter: SessionCookieWriter

    @Test
    fun `get returns the persisted account of the authenticated user`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(staleAuthUser())
        whenever(accountService.get(1L)).thenReturn(ada())

        mockMvc
            .perform(
                get("/api/me")
                    .param("id", "99")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist())

        verify(accountService).get(1L)
        verify(sessionCookieWriter, never()).write(any(), any())
    }

    @Test
    fun `get returns 401 without a session cookie`() {
        mockMvc
            .perform(get("/api/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(accountService, never()).get(any())
    }

    @Test
    fun `get returns 401 for a tampered cookie`() {
        whenever(tokenProvider.parse("tampered")).thenReturn(null)

        mockMvc
            .perform(get("/api/me").cookie(Cookie("auth_token", "tampered")))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))
    }

    @Test
    fun `get returns 401 when the authenticated account no longer exists`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(staleAuthUser())
        whenever(accountService.get(1L)).thenThrow(UserNotFoundException())

        mockMvc
            .perform(get("/api/me").cookie(Cookie("auth_token", "good-token")))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))
    }

    @Test
    fun `patch updates the authenticated account and renews the session cookie`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(staleAuthUser())
        whenever(
            accountService.update(
                userId = 1L,
                name = "  Ada Byron Lovelace  ",
                email = Helpers.emailOf("ada.lovelace@example.com"),
            ),
        ).thenReturn(
            User(
                id = 1L,
                name = "Ada Byron Lovelace",
                email = "ada.lovelace@example.com",
                passwordHash = "hashed",
            ),
        )
        whenever(tokenProvider.issue(any())).thenReturn("renewed.jwt.token")

        mockMvc
            .perform(
                patch("/api/me")
                    .param("id", "99")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "id": 99,
                          "name": "  Ada Byron Lovelace  ",
                          "email": "ada.lovelace@example.com",
                          "password": "should-be-ignored"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ada Byron Lovelace"))
            .andExpect(jsonPath("$.email").value("ada.lovelace@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist())

        verify(accountService).update(1L, "  Ada Byron Lovelace  ", Helpers.emailOf("ada.lovelace@example.com"))
        verify(tokenProvider).issue(
            check {
                assertEquals(1L, it.id)
                assertEquals("ada.lovelace@example.com", it.username)
                assertEquals("Ada Byron Lovelace", it.name)
                assertEquals("", it.password)
            },
        )
        verify(sessionCookieWriter).write(any<HttpServletResponse>(), eq("renewed.jwt.token"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"email":"ada@example.com"}""",
            """{"name":"Ada Lovelace"}""",
            """{"name":"   ","email":"ada@example.com"}""",
            """{"name":" A ","email":"ada@example.com"}""",
            """{"name":"Ada Lovelace","email":"not-an-email"}""",
            """{"name":"Ada Lovelace","email":"   "}""",
        ],
    )
    fun `patch returns 400 for invalid request data`(body: String) {
        whenever(tokenProvider.parse("good-token")).thenReturn(staleAuthUser())

        mockMvc
            .perform(
                patch("/api/me")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(accountService, never()).update(any(), any(), any())
        verify(sessionCookieWriter, never()).write(any(), any())
    }

    @Test
    fun `patch returns 409 when the email belongs to another account`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(staleAuthUser())
        whenever(accountService.update(any(), any(), any())).thenThrow(EmailAlreadyInUseException())

        mockMvc
            .perform(
                patch("/api/me")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ada Lovelace",
                          "email": "taken@example.com"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Email already in use"))

        verify(sessionCookieWriter, never()).write(any(), any())
        verify(tokenProvider, never()).issue(any())
    }

    @Test
    fun `patch returns 401 without a session cookie`() {
        mockMvc
            .perform(
                patch("/api/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ada Lovelace",
                          "email": "ada@example.com"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(accountService, never()).update(any(), any(), any())
    }

    private fun staleAuthUser(): AuthUser =
        AuthUser(
            id = 1L,
            username = "old@example.com",
            password = "",
            roles = emptyList(),
            name = "Old Name",
        )

    private fun ada(): User =
        User(
            id = 1L,
            name = "Ada Lovelace",
            email = "ada@example.com",
            passwordHash = "hashed",
        )
}
