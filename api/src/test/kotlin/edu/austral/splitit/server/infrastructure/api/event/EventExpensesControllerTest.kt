package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.CreateExpenseCommand
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.application.service.ExpensePayerSummary
import edu.austral.splitit.server.application.service.ExpenseSummary
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

@WebMvcTest(
    controllers = [EventController::class],
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
class EventExpensesControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var eventApplicationService: EventApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    private val authUser =
        AuthUser(
            id = 1L,
            username = "mateo@example.com",
            password = "",
            roles = emptyList(),
            name = "Mateo",
        )

    private fun expenseSummary() =
        ExpenseSummary(
            id = 34L,
            eventId = 7L,
            name = "Cena",
            originalAmount = BigDecimal("5000"),
            originalCurrency = "ARS",
            baseAmount = BigDecimal("5000"),
            baseCurrency = "ARS",
            paidByMember = ExpensePayerSummary(id = 12L, name = "Ana"),
            expenseDate = LocalDate.of(2026, 9, 24),
        )

    @Test
    fun `post expenses creates an expense and returns 201 with summary`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.addExpense(any())).thenReturn(expenseSummary())

        mockMvc
            .perform(
                post("/api/events/7/expenses")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Cena",
                          "amount": 5000,
                          "currency": "ARS",
                          "paidByMemberId": 12
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(34))
            .andExpect(jsonPath("$.eventId").value(7))
            .andExpect(jsonPath("$.name").value("Cena"))
            .andExpect(jsonPath("$.originalAmount").value(5000))
            .andExpect(jsonPath("$.originalCurrency").value("ARS"))
            .andExpect(jsonPath("$.baseAmount").value(5000))
            .andExpect(jsonPath("$.baseCurrency").value("ARS"))
            .andExpect(jsonPath("$.paidByMember.id").value(12))
            .andExpect(jsonPath("$.paidByMember.name").value("Ana"))
            .andExpect(jsonPath("$.expenseDate").value("2026-09-24"))

        verify(eventApplicationService).addExpense(
            check<CreateExpenseCommand> {
                assertEquals(1L, it.userId)
                assertEquals(7L, it.eventId)
                assertEquals("Cena", it.name)
                assertEquals(BigDecimal("5000"), it.amount)
                assertEquals("ARS", it.currency)
                assertEquals(12L, it.paidByMemberId)
            },
        )
    }

    @Test
    fun `post expenses returns 401 without session`() {
        mockMvc
            .perform(
                post("/api/events/7/expenses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Cena","amount":5000,"currency":"ARS","paidByMemberId":12}"""),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(eventApplicationService, never()).addExpense(any())
    }

    @Test
    fun `post expenses returns 403 when user has no access to the event`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.addExpense(any()))
            .thenThrow(AccessDeniedException("Forbidden"))

        mockMvc
            .perform(
                post("/api/events/7/expenses")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Cena","amount":5000,"currency":"ARS","paidByMemberId":12}"""),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))
    }

    @Test
    fun `post expenses returns 404 when event does not exist`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.addExpense(any()))
            .thenThrow(EventNotFoundException())

        mockMvc
            .perform(
                post("/api/events/999/expenses")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Cena","amount":5000,"currency":"ARS","paidByMemberId":12}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"amount":5000,"currency":"ARS","paidByMemberId":12}""",
            """{"name":"","amount":5000,"currency":"ARS","paidByMemberId":12}""",
            """{"name":"   ","amount":5000,"currency":"ARS","paidByMemberId":12}""",
            """{"name":"Cena","amount":0,"currency":"ARS","paidByMemberId":12}""",
            """{"name":"Cena","amount":-5,"currency":"ARS","paidByMemberId":12}""",
            """{"name":"Cena","currency":"ARS","paidByMemberId":12}""",
            """{"name":"Cena","amount":5000,"currency":"AR","paidByMemberId":12}""",
            """{"name":"Cena","amount":5000,"currency":"123","paidByMemberId":12}""",
            """{"name":"Cena","amount":5000,"currency":"ARS"}""",
        ],
    )
    fun `post expenses returns 400 for invalid body`(body: String) {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)

        mockMvc
            .perform(
                post("/api/events/7/expenses")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(eventApplicationService, never()).addExpense(any())
    }

    @Test
    fun `get expenses returns list of event expenses with 200`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listExpenses(1L, 7L)).thenReturn(listOf(expenseSummary()))

        mockMvc
            .perform(
                get("/api/events/7/expenses")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(34))
            .andExpect(jsonPath("$[0].name").value("Cena"))
            .andExpect(jsonPath("$[0].paidByMember.name").value("Ana"))

        verify(eventApplicationService).listExpenses(1L, 7L)
    }

    @Test
    fun `get expenses returns 401 without session`() {
        mockMvc
            .perform(get("/api/events/7/expenses"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(eventApplicationService, never()).listExpenses(any(), any())
    }

    @Test
    fun `get expenses returns 403 when user has no access to the event`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listExpenses(any(), any()))
            .thenThrow(AccessDeniedException("Forbidden"))

        mockMvc
            .perform(
                get("/api/events/7/expenses")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))
    }

    @Test
    fun `get expenses returns 404 when event does not exist`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listExpenses(any(), any()))
            .thenThrow(EventNotFoundException())

        mockMvc
            .perform(
                get("/api/events/999/expenses")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }
}
