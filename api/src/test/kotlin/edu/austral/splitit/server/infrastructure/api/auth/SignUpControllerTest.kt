package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.ServletException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
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
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@Import(GlobalExceptionHandler::class)
class SignUpControllerTest(
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
    fun `register returns 201 with public user fields only`() {
        whenever(
            signUpService.register(
                "Ada Lovelace",
                Helpers.emailOf("ada@example.com"),
                "una-clave-segura",
            ),
        ).thenReturn(
            User(
                id = 1L,
                name = "Ada Lovelace",
                email = "ada@example.com",
                passwordHash = "hashed",
            ),
        )

        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ada Lovelace",
                          "email": "ada@example.com",
                          "password": "una-clave-segura"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.user").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"email":"ada@example.com","password":"una-clave-segura"}""",
            """{"name":"   ","email":"ada@example.com","password":"una-clave-segura"}""",
            """{"name":" A ","email":"ada@example.com","password":"una-clave-segura"}""",
            """{"name":" Ada Lovelace ","email":"ada@example.com","password":"una-clave-segura"}""",
            """{"name":"Ada Lovelace","email":"not-an-email","password":"una-clave-segura"}""",
            """{"name":"Ada Lovelace","email":"ada@example.com","password":"short"}""",
        ],
    )
    fun `register returns 400 for invalid request data`(body: String) {
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(signUpService, never()).register(any(), any(), any())
    }

    @Test
    fun `register returns 400 when Email create rejects the address`() {
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ada Lovelace",
                          "email": "user@exam_ple.com",
                          "password": "una-clave-segura"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(signUpService, never()).register(any(), any(), any())
    }

    @Test
    fun `register returns 409 when email is already in use`() {
        whenever(signUpService.register(any(), any(), any())).thenThrow(EmailAlreadyInUseException())

        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ada Lovelace",
                          "email": "ada@example.com",
                          "password": "una-clave-segura"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Email already in use"))
    }

    @Test
    fun `unexpected errors return 500 without leaking internals`() {
        whenever(signUpService.register(any(), any(), any()))
            .thenThrow(IllegalStateException("jdbc:postgresql://secret-host/splitit"))

        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ada Lovelace",
                          "email": "ada@example.com",
                          "password": "una-clave-segura"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("Internal server error"))
    }

    @Test
    fun `access denied is not mapped to a generic 500`() {
        whenever(signUpService.register(any(), any(), any())).thenThrow(AccessDeniedException("denied"))

        val thrown =
            assertFailsWith<ServletException> {
                mockMvc
                    .perform(
                        post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "name": "Ada Lovelace",
                                  "email": "ada@example.com",
                                  "password": "una-clave-segura"
                                }
                                """.trimIndent(),
                            ),
                    ).andReturn()
            }

        assertIs<AccessDeniedException>(thrown.cause)
    }
}
