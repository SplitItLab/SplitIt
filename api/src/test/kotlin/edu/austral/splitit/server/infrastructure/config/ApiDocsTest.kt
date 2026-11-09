package edu.austral.splitit.server.infrastructure.config

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.api.StatusController
import edu.austral.splitit.server.infrastructure.security.USER_ROLE
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.core.configuration.SpringDocKotlinConfiguration
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.properties.SwaggerUiOAuthProperties
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springdoc.webmvc.ui.SwaggerConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// Mirrors the starter's auto-configuration, which @WebMvcTest does not load.
@ImportAutoConfiguration(
    SpringDocConfiguration::class,
    SpringDocConfigProperties::class,
    SpringDocKotlinConfiguration::class,
    SpringDocWebMvcConfiguration::class,
    MultipleOpenApiSupportConfiguration::class,
    SwaggerConfig::class,
    SwaggerUiConfigProperties::class,
    SwaggerUiOAuthProperties::class,
)
annotation class SpringDocAutoConfigurations

private const val BASE_PROPERTIES = "jwt.secret=test-jwt-secret-that-is-long-enough"

@WebMvcTest(
    controllers = [StatusController::class],
    excludeAutoConfiguration = [UserDetailsServiceAutoConfiguration::class],
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@SpringDocAutoConfigurations
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "springdoc.api-docs.path=/api/v3/api-docs",
        "springdoc.swagger-ui.path=/api/swagger-ui.html",
        // The production default: API_DOCS_ENABLED unset resolves to false.
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false",
        BASE_PROPERTIES,
    ],
)
class ApiDocsDisabledTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    @Test
    fun `docs routes answer 404 with a session when the environment variable is not set`() {
        whenever(
            tokenProvider.parse("good-token"),
        ).thenReturn(
            AuthUser(
                id = 1L,
                username = "ada@example.com",
                password = "",
                roles = listOf(USER_ROLE),
                name = "Ada",
            ),
        )
        val session = Cookie("auth_token", "good-token")

        mockMvc.perform(get("/api/v3/api-docs").cookie(session)).andExpect(status().isNotFound())
        mockMvc.perform(get("/api/swagger-ui.html").cookie(session)).andExpect(status().isNotFound())
    }
}

@WebMvcTest(
    controllers = [StatusController::class],
    excludeAutoConfiguration = [UserDetailsServiceAutoConfiguration::class],
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@SpringDocAutoConfigurations
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "springdoc.api-docs.path=/api/v3/api-docs",
        "springdoc.swagger-ui.path=/api/swagger-ui.html",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        BASE_PROPERTIES,
    ],
)
class ApiDocsEnabledTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    @Test
    fun `docs routes answer without a session when enabled`() {
        mockMvc.perform(get("/api/v3/api-docs")).andExpect(status().isOk())
        mockMvc.perform(get("/api/swagger-ui/index.html")).andExpect(status().isOk())
    }
}
