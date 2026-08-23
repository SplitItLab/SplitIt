package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class SignUpService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(
        name: String,
        email: String,
        password: String,
    ): User {
        val normalizedEmail = email.trim().lowercase()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw EmailAlreadyInUseException()
        }

        val passwordHash = requireNotNull(passwordEncoder.encode(password))

        val user =
            User(
                name = name,
                email = normalizedEmail,
                passwordHash = passwordHash,
            )

        return try {
            userRepository.save(user)
        } catch (_: DataIntegrityViolationException) {
            throw EmailAlreadyInUseException()
        }
    }
}
