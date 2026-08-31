package edu.austral.splitit.server

import edu.austral.splitit.server.domain.model.user.Email

object Helpers {
    fun emailOf(email: String): Email =
        Email.create(email).getOrNull()
            ?: throw IllegalArgumentException("Invalid email")
}
