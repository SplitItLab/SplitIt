package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class SignUpService(
    private val passwordEncoder: PasswordEncoder,
    private val userService: UserService,
) {
    fun register(
        name: String,
        email: String,
        password: String,
    ): User {
        val normalizedEmail = email.trim().lowercase()

        if (userService.findByEmail(normalizedEmail) != null) {
            throw EmailAlreadyInUseException()
        }

        val passwordHash =
            requireNotNull(
                passwordEncoder
                    .encode(password),
            )

        return userService.save(
            name = name,
            email = normalizedEmail,
            passwordHash = passwordHash,
        )
    }
}
