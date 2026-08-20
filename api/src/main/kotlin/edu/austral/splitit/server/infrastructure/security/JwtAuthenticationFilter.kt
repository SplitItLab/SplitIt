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
    private val securityContextRepository: SecurityContextRepository = RequestAttributeSecurityContextRepository(),
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val token = header.substring(BEARER_PREFIX.length)
            val authenticatedUser = tokenProvider.parse(token)
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
        }
        filterChain.doFilter(request, response)
    }
}
