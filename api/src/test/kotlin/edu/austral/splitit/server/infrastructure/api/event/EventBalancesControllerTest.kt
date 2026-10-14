package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.BalanceSummary
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(
    controllers = [EventBalanceController::class],
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
class EventBalancesControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var eventApplicationService: EventApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    private val authUser =
        AuthUser(
            id = 1L,
            username = "ana@example.com",
            password = "",
            roles = emptyList(),
            name = "Ana",
        )

    @Test
    fun `get balances returns each member net balance`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listBalances(1L, 7L)).thenReturn(
            listOf(
                BalanceSummary(memberId = 1L, memberName = "Ana", netBalance = BigDecimal("50.0000")),
                BalanceSummary(memberId = 2L, memberName = "Luis", netBalance = BigDecimal("-50.0000")),
            ),
        )

        mockMvc
            .perform(
                get("/api/events/7/balances")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].memberId").value(1))
            .andExpect(jsonPath("$[0].memberName").value("Ana"))
            .andExpect(jsonPath("$[0].netBalance").value(50.0))
            .andExpect(jsonPath("$[1].memberId").value(2))
            .andExpect(jsonPath("$[1].memberName").value("Luis"))
            .andExpect(jsonPath("$[1].netBalance").value(-50.0))

        verify(eventApplicationService).listBalances(1L, 7L)
    }

    @Test
    fun `get balances returns 401 without session`() {
        mockMvc
            .perform(get("/api/events/7/balances"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(eventApplicationService, never()).listBalances(any(), any())
    }

    @Test
    fun `get balances returns 403 when user has no access to the event`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listBalances(any(), any()))
            .thenThrow(AccessDeniedException("Forbidden"))

        mockMvc
            .perform(
                get("/api/events/7/balances")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))
    }

    @Test
    fun `get balances returns 404 when event does not exist`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listBalances(any(), any()))
            .thenThrow(EventNotFoundException())

        mockMvc
            .perform(
                get("/api/events/999/balances")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }
}
