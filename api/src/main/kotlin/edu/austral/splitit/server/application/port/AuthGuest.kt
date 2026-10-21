package edu.austral.splitit.server.application.port

data class AuthGuest(
    val eventId: Long,
    val memberId: Long,
    val name: String,
)
