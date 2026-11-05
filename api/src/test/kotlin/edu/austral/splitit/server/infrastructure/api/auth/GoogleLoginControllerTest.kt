package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.application.exception.PasswordAccountExistsException
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.ExistingAccountChoice
import edu.austral.splitit.server.application.service.GoogleLoginService
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
    ],
)
class GoogleLoginControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var signUpService: SignUpService

    @MockitoBean
    private lateinit var loginService: LoginService

    @MockitoBean
    private lateinit var googleLoginService: GoogleLoginService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `google login returns 200 with the public user and the same session cookie as the password login`() {
        whenever(googleLoginService.login(eq("google.id.token"), isNull())).thenReturn(Helpers.googleUser())
        whenever(tokenProvider.issue(any())).thenReturn("signed.jwt.token")

        mockMvc
            .perform(post("/api/auth/google").json("""{"credential":"google.id.token"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(
                header().string(
                    HttpHeaders.SET_COOKIE,
                    allOf(
                        containsString("auth_token=signed.jwt.token"),
                        containsString("HttpOnly"),
                        containsString("Path=/"),
                        containsString("Max-Age=28800"),
                    ),
                ),
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["LINK", "REPLACE"])
    fun `google login passes the chosen action to the service`(choice: String) {
        whenever(googleLoginService.login(any(), anyOrNull())).thenReturn(Helpers.googleUser())
        whenever(tokenProvider.issue(any())).thenReturn("signed.jwt.token")

        mockMvc
            .perform(post("/api/auth/google").json("""{"credential":"google.id.token","existingAccount":"$choice"}"""))
            .andExpect(status().isOk)

        verify(googleLoginService).login("google.id.token", ExistingAccountChoice.valueOf(choice))
    }

    @Test
    fun `google login returns 409 when an account with a password must be chosen and sets no cookie`() {
        whenever(googleLoginService.login(any(), anyOrNull())).thenThrow(PasswordAccountExistsException())

        mockMvc
            .perform(post("/api/auth/google").json("""{"credential":"google.id.token"}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Account already exists with a password"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))

        verify(tokenProvider, never()).issue(any())
    }

    @Test
    fun `google login returns 401 for an invalid credential`() {
        whenever(googleLoginService.login(any(), anyOrNull())).thenThrow(InvalidCredentialsException())

        mockMvc
            .perform(post("/api/auth/google").json("""{"credential":"forged"}"""))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid credentials"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{}""",
            """{"credential":"   "}""",
            """{"credential":"google.id.token","existingAccount":"MERGE"}""",
        ],
    )
    fun `google login returns 400 for invalid request data`(body: String) {
        mockMvc
            .perform(post("/api/auth/google").json(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(googleLoginService, never()).login(any(), anyOrNull())
    }

    private fun org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder.json(body: String) =
        contentType(MediaType.APPLICATION_JSON).content(body)
}
