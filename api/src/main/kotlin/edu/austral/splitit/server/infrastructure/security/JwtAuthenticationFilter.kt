package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter

const val BEARER_PREFIX = "Bearer "
const val USER_ROLE = "USER"
const val GUEST_ROLE = "GUEST"

class JwtAuthenticationFilter(
    private val tokenProvider: TokenProvider,
    private val guestAccessService: GuestAccessService,
    private val cookieName: String,
    private val securityContextRepository: SecurityContextRepository = RequestAttributeSecurityContextRepository(),
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // A user session wins over a guest credential sent alongside it.
        val authentication = authenticateUser(request) ?: authenticateGuest(request)
        if (authentication != null) {
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = authentication
            SecurityContextHolder.setContext(context)
            securityContextRepository.saveContext(context, request, response)
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticateUser(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val user =
            listOfNotNull(cookieToken(request, cookieName), bearerToken(request))
                .firstNotNullOfOrNull { tokenProvider.parse(it) }
                ?: return null
        // Roles come from the token kind, not its claims: live user tokens carry no roles.
        val roles = listOf(USER_ROLE) + user.roles
        return UsernamePasswordAuthenticationToken(user, null, roles.map { SimpleGrantedAuthority("ROLE_$it") })
    }

    private fun authenticateGuest(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val guest =
            listOfNotNull(cookieToken(request, GUEST_COOKIE_NAME), bearerToken(request))
                .firstNotNullOfOrNull { token ->
                    tokenProvider.parseGuest(token)?.takeIf { guestAccessService.isActive(it) }
                }
                ?: return null
        return UsernamePasswordAuthenticationToken(guest, null, listOf(SimpleGrantedAuthority("ROLE_$GUEST_ROLE")))
    }

    private fun cookieToken(
        request: HttpServletRequest,
        name: String,
    ): String? =
        request.cookies
            ?.firstOrNull { it.name == name }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun bearerToken(request: HttpServletRequest): String? =
        request
            .getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
