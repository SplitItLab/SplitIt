package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtAuthenticationFilterTest {
    private val tokenProvider: TokenProvider = mock()
    private val filter = JwtAuthenticationFilter(tokenProvider, COOKIE_NAME)

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
    }
}
