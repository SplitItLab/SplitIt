package edu.austral.splitit.server

import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.domain.model.user.User

object Helpers {
    fun emailOf(email: String): Email =
        Email.create(email).getOrNull()
            ?: throw IllegalArgumentException("Invalid email")

    fun user(
        name: String = "Ada Lovelace",
        email: String = "ada@example.com",
        passwordHash: String = "hashed",
        id: Long? = 1L,
    ): User =
        User
            .create(
                name = name,
                email = emailOf(email),
                passwordHash = passwordHash,
            ).apply { this.id = id }
}
