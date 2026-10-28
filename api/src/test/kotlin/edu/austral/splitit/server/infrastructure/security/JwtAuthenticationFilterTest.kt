package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtAuthenticationFilterTest {
    private val tokenProvider: TokenProvider = mock()
    private val guestAccessService: GuestAccessService =
        mock {
            on { isActive(any()) } doReturn true
        }
    private val filter = JwtAuthenticationFilter(tokenProvider, guestAccessService, COOKIE_NAME)
    private val realTokenProvider = JwtTokenProvider(SECRET, EXPIRATION_HOURS, Clock.systemUTC())

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid cookie authenticates the request`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(COOKIE_NAME, "good-token"))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(adaAuthUser(), authentication.principal)
        assertTrue(authentication.isAuthenticated)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `valid bearer token authenticates when there is no cookie`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(adaAuthUser(), authentication.principal)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `valid bearer token authenticates when the session cookie is stale`() {
        whenever(tokenProvider.parse("bad-token")).thenReturn(null)
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(COOKIE_NAME, "bad-token"))
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(adaAuthUser(), authentication.principal)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `expired or tampered token leaves the request unauthenticated`() {
        whenever(tokenProvider.parse("bad-token")).thenReturn(null)
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(COOKIE_NAME, "bad-token"))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `missing token leaves the request unauthenticated`() {
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `a user token grants the user role even without role claims`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(COOKIE_NAME, "good-token"))

        filter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertEquals(listOf("ROLE_USER"), authorities())
    }

    @Test
    fun `valid guest cookie authenticates the request as a guest`() {
        whenever(tokenProvider.parseGuest("guest-token")).thenReturn(juanGuest())
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(GUEST_COOKIE_NAME, "guest-token"))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertEquals(juanGuest(), SecurityContextHolder.getContext().authentication?.principal)
        assertEquals(listOf("ROLE_GUEST"), authorities())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `a guest credential stops authenticating once its member has an account`() {
        whenever(tokenProvider.parseGuest("guest-token")).thenReturn(juanGuest())
        whenever(guestAccessService.isActive(juanGuest())).thenReturn(false)
        val chain = mock<FilterChain>()
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(GUEST_COOKIE_NAME, "guest-token"))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `a valid guest bearer token authenticates when the guest cookie is no longer active`() {
        val staleGuest = AuthGuest(eventId = 10L, memberId = 31L, name = "Sofi")
        whenever(tokenProvider.parseGuest("stale-guest-token")).thenReturn(staleGuest)
        whenever(tokenProvider.parseGuest("guest-token")).thenReturn(juanGuest())
        whenever(guestAccessService.isActive(staleGuest)).thenReturn(false)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(GUEST_COOKIE_NAME, "stale-guest-token"))
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer guest-token")

        filter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertEquals(juanGuest(), SecurityContextHolder.getContext().authentication?.principal)
    }

    @Test
    fun `the user session wins over a guest credential`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(adaAuthUser())
        whenever(tokenProvider.parseGuest("guest-token")).thenReturn(juanGuest())
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(COOKIE_NAME, "good-token"), Cookie(GUEST_COOKIE_NAME, "guest-token"))

        filter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertEquals(adaAuthUser(), SecurityContextHolder.getContext().authentication?.principal)
        assertEquals(listOf("ROLE_USER"), authorities())
    }

    @Test
    fun `a guest token in the session cookie does not authenticate`() {
        val realFilter = JwtAuthenticationFilter(realTokenProvider, guestAccessService, COOKIE_NAME)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(COOKIE_NAME, realTokenProvider.issueGuest(juanGuest())))

        realFilter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `a user token in the guest cookie does not authenticate`() {
        val realFilter = JwtAuthenticationFilter(realTokenProvider, guestAccessService, COOKIE_NAME)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(GUEST_COOKIE_NAME, realTokenProvider.issue(adaAuthUser())))

        realFilter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `a guest bearer token authenticates as a guest, never as a user`() {
        val realFilter = JwtAuthenticationFilter(realTokenProvider, guestAccessService, COOKIE_NAME)
        val request = MockHttpServletRequest()
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${realTokenProvider.issueGuest(juanGuest())}")

        realFilter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertEquals(juanGuest(), SecurityContextHolder.getContext().authentication?.principal)
        assertEquals(listOf("ROLE_GUEST"), authorities())
    }

    @Test
    fun `a user bearer token authenticates as a user, never as a guest`() {
        val realFilter = JwtAuthenticationFilter(realTokenProvider, guestAccessService, COOKIE_NAME)
        val request = MockHttpServletRequest()
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${realTokenProvider.issue(adaAuthUser())}")

        realFilter.doFilter(request, MockHttpServletResponse(), mock<FilterChain>())

        assertEquals(adaAuthUser(), SecurityContextHolder.getContext().authentication?.principal)
        assertEquals(listOf("ROLE_USER"), authorities())
    }

    private fun authorities(): List<String> =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            ?.mapNotNull { it.authority }
            .orEmpty()

    private fun juanGuest(): AuthGuest = AuthGuest(eventId = 10L, memberId = 30L, name = "Juan")

    private fun adaAuthUser(): AuthUser =
        AuthUser(
            id = 1L,
            username = "ada@example.com",
            password = "",
            roles = emptyList(),
            name = "Ada Lovelace",
        )

    private companion object {
        const val COOKIE_NAME = "auth_token"
        const val SECRET = "test-jwt-secret-that-is-long-enough"
        const val EXPIRATION_HOURS = 8L
    }
}
