package edu.austral.splitit.server.application.port

data class AuthUser(
    val id: Long,
    val username: String,
    val password: String,
    val roles: Collection<String>,
    val name: String,
)
