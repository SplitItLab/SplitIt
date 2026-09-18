package edu.austral.splitit.server.infrastructure.api.event

data class InviteLinkResponse(
    val token: String,
) {
    companion object {
        fun of(token: String): InviteLinkResponse = InviteLinkResponse(token)
    }
}
