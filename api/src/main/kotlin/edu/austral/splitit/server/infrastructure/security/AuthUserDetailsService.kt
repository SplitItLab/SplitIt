package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AuthUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val email = username.trim().lowercase()
        val user =
            userRepository.findByEmail(email)
                ?: throw UsernameNotFoundException(email)
        return User(
            user.email,
            user.passwordHash,
            emptyList(),
        )
    }
}
