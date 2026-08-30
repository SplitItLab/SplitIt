package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.domain.model.user.User
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
        email: Email,
        password: String,
    ): User {
        if (userService.findByEmail(email) != null) {
            throw EmailAlreadyInUseException()
        }

        val passwordHash =
            requireNotNull(
                passwordEncoder
                    .encode(password),
            )

        return userService.save(
            name = name,
            email = email,
            passwordHash = passwordHash,
        )
    }
}
