package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.domain.service.UserService
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class SignUpService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userService: UserService,
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

        val passwordHash =
            requireNotNull(
                passwordEncoder
                    .encode(password),
            )

        val user =
            userService.create(
                name = name,
                email = normalizedEmail,
                passwordHash = passwordHash,
            )

        return saveNewUser(user)
    }

    private fun saveNewUser(user: User): User =
        try {
            userRepository.save(user)
        } catch (exception: DataIntegrityViolationException) {
            if (!isEmailUniqueConstraintViolation(exception)) {
                throw exception
            }
            throw EmailAlreadyInUseException()
        }

    private fun isEmailUniqueConstraintViolation(exception: DataIntegrityViolationException): Boolean {
        var current: Throwable? = exception
        val seen = mutableSetOf<Throwable>()
        while (current != null && seen.add(current)) {
            val message = current.message
            if (message != null && message.contains(EMAIL_UNIQUE_CONSTRAINT, ignoreCase = true)) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        const val EMAIL_UNIQUE_CONSTRAINT = "uk_users_email"
    }
}
