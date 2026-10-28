package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.BalanceApplicationService
import edu.austral.splitit.server.application.service.BalanceSummary
import edu.austral.splitit.server.application.service.CreateExpenseCommand
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.application.service.EventDetail
import edu.austral.splitit.server.application.service.ExpenseApplicationService
import edu.austral.splitit.server.application.service.ExpensePayerSummary
import edu.austral.splitit.server.application.service.ExpenseSummary
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.application.service.QuoteExpenseQuery
import edu.austral.splitit.server.application.service.SettlementApplicationService
import edu.austral.splitit.server.domain.model.event.Currency
import edu.austral.splitit.server.domain.model.event.ExchangeQuote
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

// SPT-98: what a guest credential can and cannot do on each event endpoint.
@WebMvcTest(
    controllers = [EventController::class, EventBalanceController::class, EventSettlementController::class],
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
class EventGuestAccessTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var eventApplicationService: EventApplicationService

    @MockitoBean
    private lateinit var expenseApplicationService: ExpenseApplicationService

    @MockitoBean
    private lateinit var balanceApplicationService: BalanceApplicationService

    @MockitoBean
    private lateinit var settlementApplicationService: SettlementApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    private val guest = AuthGuest(eventId = 10L, memberId = 30L, name = "Juan")

    @BeforeEach
    fun authenticateGuest() {
        whenever(tokenProvider.parseGuest(GUEST_TOKEN)).thenReturn(guest)
        whenever(guestAccessService.isActive(guest)).thenReturn(true)
    }

    @Test
    fun `a guest sees the event and is told which member it is`() {
        whenever(eventApplicationService.getEventById(guest, 10L)).thenReturn(
            EventDetail(
                id = 10L,
                name = "Viaje a Bariloche",
                description = null,
                iconKey = null,
                baseCurrency = "ARS",
                memberCount = 0L,
                members = emptyList(),
                isOwner = false,
                guestMemberId = 30L,
            ),
        )

        mockMvc
            .perform(get("/api/events/10").cookie(guestCookie()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isOwner").value(false))
            .andExpect(jsonPath("$.guestMemberId").value(30))
    }

    @Test
    fun `a guest sees the expenses`() {
        whenever(expenseApplicationService.listExpenses(guest, 10L)).thenReturn(listOf(expenseSummary()))

        mockMvc
            .perform(get("/api/events/10/expenses").cookie(guestCookie()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(34))
    }

    @Test
    fun `a guest sees the balances`() {
        whenever(balanceApplicationService.listBalances(guest, 10L)).thenReturn(
            listOf(BalanceSummary(memberId = 30L, memberName = "Juan", netBalance = BigDecimal("50.0000"))),
        )

        mockMvc
            .perform(get("/api/events/10/balances").cookie(guestCookie()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].memberId").value(30))
    }

    @Test
    fun `a guest sees the suggested payments`() {
        whenever(settlementApplicationService.listSuggestedPayments(guest, 10L)).thenReturn(emptyList())

        mockMvc
            .perform(get("/api/events/10/suggested-payments").cookie(guestCookie()))
            .andExpect(status().isOk)
    }

    @Test
    fun `a guest quotes an amount in another currency`() {
        whenever(expenseApplicationService.quoteExpense(any())).thenReturn(
            ExchangeQuote.create(
                originalAmount = BigDecimal("10"),
                originalCurrency = Currency("USD"),
                baseCurrency = Currency("ARS"),
                rate = BigDecimal("1523.9662"),
            ),
        )

        mockMvc
            .perform(
                get("/api/events/10/expenses/quote")
                    .param("amount", "10")
                    .param("currency", "USD")
                    .cookie(guestCookie()),
            ).andExpect(status().isOk)

        verify(expenseApplicationService).quoteExpense(check<QuoteExpenseQuery> { assertEquals(guest, it.principal) })
    }

    @Test
    fun `a guest adds an expense paid by its member`() {
        whenever(expenseApplicationService.addExpense(any())).thenReturn(expenseSummary())

        mockMvc
            .perform(
                post("/api/events/10/expenses")
                    .cookie(guestCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Cena","amount":5000,"currency":"ARS","paidByMemberId":30}"""),
            ).andExpect(status().isCreated)

        verify(expenseApplicationService).addExpense(check<CreateExpenseCommand> { assertEquals(guest, it.principal) })
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(
        "PUT, /api/events/10/expenses/34",
        "DELETE, /api/events/10/expenses/34",
        "PUT, /api/events/10",
        "DELETE, /api/events/10",
        "POST, /api/events/10/invite-link",
        "GET, /api/events",
        "POST, /api/events",
    )
    fun `a guest is forbidden from the owner and account-member actions`(
        method: String,
        path: String,
    ) {
        mockMvc
            .perform(
                request(HttpMethod.valueOf(method), path)
                    .cookie(guestCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"Cena","amount":5000,"currency":"ARS","paidByMemberId":30,"baseCurrency":"ARS"}""",
                    ),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Forbidden"))

        verifyNoInteractions(eventApplicationService, expenseApplicationService)
    }

    @Test
    fun `a guest credential whose member now has an account is rejected`() {
        whenever(guestAccessService.isActive(guest)).thenReturn(false)

        mockMvc
            .perform(get("/api/events/10").cookie(guestCookie()))
            .andExpect(status().isUnauthorized)

        verifyNoInteractions(eventApplicationService)
    }

    private fun guestCookie() = Cookie("guest_token", GUEST_TOKEN)

    private fun expenseSummary() =
        ExpenseSummary(
            id = 34L,
            eventId = 10L,
            name = "Cena",
            originalAmount = BigDecimal("5000"),
            originalCurrency = "ARS",
            exchangeRate = BigDecimal("1.000000"),
            baseAmount = BigDecimal("5000.0000"),
            baseCurrency = "ARS",
            paidByMember = ExpensePayerSummary(id = 30L, name = "Juan"),
            expenseDate = LocalDate.of(2026, 9, 24),
        )

    private companion object {
        const val GUEST_TOKEN = "guest-token"
    }
}
