package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.port.AuthGuest

data class GuestSessionResponse(
    val eventId: Long,
    val memberId: Long,
    val name: String,
) {
    // Lets the web tell a guest session apart from a user session.
    val type: String = "guest"

    companion object {
        fun of(guest: AuthGuest): GuestSessionResponse =
            GuestSessionResponse(
                eventId = guest.eventId,
                memberId = guest.memberId,
                name = guest.name,
            )
    }
}
