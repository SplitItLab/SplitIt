package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class LoginService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun login(
        email: String,
        password: String,
    ): User {
        val normalizedEmail = email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
        if (user == null || !passwordEncoder.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return user
    }
}
