package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.User
import org.springframework.stereotype.Service

@Service
class UserService {
    fun create(
        name: String,
        email: String,
        passwordHash: String,
    ): User {
        val normalizedName = name.trim()
        require(normalizedName.length in User.NAME_MIN..User.NAME_MAX) {
            "El nombre debe tener entre ${User.NAME_MIN} y ${User.NAME_MAX} caracteres"
        }

        return User(
            name = normalizedName,
            email = email.lowercase().trim(),
            passwordHash = passwordHash,
        )
    }
}
