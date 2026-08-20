package edu.austral.splitit.server.infrastructure.security

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AuthUserDetailsService : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails = throw UsernameNotFoundException(username)
}
