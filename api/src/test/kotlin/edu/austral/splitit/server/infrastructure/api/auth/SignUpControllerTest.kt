package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
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
@Import(GlobalExceptionHandler::class)
class SignUpControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var signUpService: SignUpService

    @Test
    fun `register returns 201 with public user fields only`() {
        whenever(
            signUpService.register(
                eq("Ada Lovelace"),
                eq("ada@example.com"),
                eq("una-clave-segura"),
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
            .andExpect(jsonPath("$.user.id").value(1))
            .andExpect(jsonPath("$.user.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.user.email").value("ada@example.com"))
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"email":"ada@example.com","password":"una-clave-segura"}""",
            """{"name":"   ","email":"ada@example.com","password":"una-clave-segura"}""",
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
}
