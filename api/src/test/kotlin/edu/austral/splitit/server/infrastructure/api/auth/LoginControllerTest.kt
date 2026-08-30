package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
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
    ],
)
class LoginControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var signUpService: SignUpService

    @MockitoBean
    private lateinit var loginService: LoginService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `login returns 200 with public user fields and an HttpOnly session cookie`() {
        whenever(loginService.login(Helpers.emailOf("ada@example.com"), "una-clave-segura")).thenReturn(ada())
        whenever(tokenProvider.issue(any())).thenReturn("signed.jwt.token")

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "ada@example.com",
                          "password": "una-clave-segura"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(
                header().string(
                    HttpHeaders.SET_COOKIE,
                    allOf(
                        containsString("auth_token=signed.jwt.token"),
                        containsString("HttpOnly"),
                        containsString("Path=/"),
                        containsString("SameSite=Lax"),
                        containsString("Max-Age=28800"),
                        not(containsString("Secure")),
                    ),
                ),
            )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"password":"una-clave-segura"}""",
            """{"email":"ada@example.com"}""",
            """{"email":"   ","password":"una-clave-segura"}""",
            """{"email":"not-an-email","password":"una-clave-segura"}""",
            """{"email":"ada@example.com","password":"   "}""",
        ],
    )
    fun `login returns 400 for invalid request data`(body: String) {
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(loginService, never()).login(any(), any())
        verify(tokenProvider, never()).issue(any())
    }

    @ParameterizedTest
    @CsvSource(
        "nobody@example.com, una-clave-segura",
        "ada@example.com, wrong-password",
    )
    fun `login returns the same 401 for unknown email and wrong password`(
        email: String,
        password: String,
    ) {
        whenever(loginService.login(any(), any())).thenThrow(InvalidCredentialsException())

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "$email",
                          "password": "$password"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid credentials"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))

        verify(tokenProvider, never()).issue(any())
    }

    private fun ada(): User =
        User(
            id = 1L,
            name = "Ada Lovelace",
            email = "ada@example.com",
            passwordHash = "hashed",
        )
}
