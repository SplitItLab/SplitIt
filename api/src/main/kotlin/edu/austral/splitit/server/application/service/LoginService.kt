package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class LoginService(
    private val passwordEncoder: PasswordEncoder,
    private val userService: UserService,
) {
    fun login(
        email: Email,
        password: String,
    ): User {
        val user = userService.findByEmail(email)

        val passwordHash = user?.passwordHash ?: DUMMY_PASSWORD_HASH
        val passwordMatches =
            passwordEncoder
                .matches(password, passwordHash)

        if (user == null || !passwordMatches) {
            throw InvalidCredentialsException()
        }

        return user
    }

    private companion object {
        // Valid BCrypt hash so unknown emails still pay the same verification cost.
        private const val DUMMY_PASSWORD_HASH =
            "\$2a\$10\$wwreR4a82.lyd33Ns7XFRuDHwhHZvpa0Z/JFKECz8OPIKmI3qALNW"
    }
}
