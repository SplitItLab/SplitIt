package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.application.service.SettlementApplicationService
import edu.austral.splitit.server.application.service.SuggestedPaymentMemberSummary
import edu.austral.splitit.server.application.service.SuggestedPaymentSummary
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
    controllers = [EventSettlementController::class],
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
class EventSettlementControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var settlementApplicationService: SettlementApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    private val authUser =
        AuthUser(
            id = 1L,
            username = "mateo@example.com",
            password = "",
            roles = emptyList(),
            name = "Mateo",
        )

    @Test
    fun `get suggested payments returns who pays whom and how much with 200`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(settlementApplicationService.listSuggestedPayments(1L, 7L)).thenReturn(
            listOf(
                SuggestedPaymentSummary(
                    fromMember = SuggestedPaymentMemberSummary(id = 13L, name = "Juan"),
                    toMember = SuggestedPaymentMemberSummary(id = 12L, name = "Ana"),
                    amount = BigDecimal("2500.0000"),
                ),
            ),
        )

        mockMvc
            .perform(
                get("/api/events/7/suggested-payments")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fromMember.id").value(13))
            .andExpect(jsonPath("$[0].fromMember.name").value("Juan"))
            .andExpect(jsonPath("$[0].toMember.id").value(12))
            .andExpect(jsonPath("$[0].toMember.name").value("Ana"))
            .andExpect(jsonPath("$[0].amount").value(2500.0))

        verify(settlementApplicationService).listSuggestedPayments(1L, 7L)
    }

    @Test
    fun `get suggested payments returns an empty list with 200 when the split is settled`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(settlementApplicationService.listSuggestedPayments(1L, 7L)).thenReturn(emptyList())

        mockMvc
            .perform(
                get("/api/events/7/suggested-payments")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `get suggested payments returns 401 without session`() {
        mockMvc
            .perform(get("/api/events/7/suggested-payments"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(settlementApplicationService, never()).listSuggestedPayments(any(), any())
    }

    @Test
    fun `get suggested payments returns 403 when user has no access to the event`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(settlementApplicationService.listSuggestedPayments(any(), any()))
            .thenThrow(AccessDeniedException("Forbidden"))

        mockMvc
            .perform(
                get("/api/events/7/suggested-payments")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))
    }

    @Test
    fun `get suggested payments returns 404 when event does not exist`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(settlementApplicationService.listSuggestedPayments(any(), any()))
            .thenThrow(EventNotFoundException())

        mockMvc
            .perform(
                get("/api/events/999/suggested-payments")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }
}
