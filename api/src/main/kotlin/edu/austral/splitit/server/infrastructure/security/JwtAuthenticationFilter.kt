package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.TokenProvider
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

class JwtAuthenticationFilter(
    private val tokenProvider: TokenProvider,
    private val cookieName: String,
    private val securityContextRepository: SecurityContextRepository = RequestAttributeSecurityContextRepository(),
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authenticatedUser =
            extractTokens(request).firstNotNullOfOrNull { tokenProvider.parse(it) }
        if (authenticatedUser != null) {
            val authentication =
                UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    authenticatedUser.roles.map { SimpleGrantedAuthority("ROLE_$it") },
                )
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = authentication
            SecurityContextHolder.setContext(context)
            securityContextRepository.saveContext(context, request, response)
        }
        filterChain.doFilter(request, response)
    }

    private fun extractTokens(request: HttpServletRequest): List<String> {
        val cookieToken =
            request.cookies
                ?.firstOrNull { it.name == cookieName }
                ?.value
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        val headerToken =
            request
                .getHeader(HttpHeaders.AUTHORIZATION)
                ?.takeIf { it.startsWith(BEARER_PREFIX) }
                ?.substring(BEARER_PREFIX.length)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        return listOfNotNull(cookieToken, headerToken)
    }
}
