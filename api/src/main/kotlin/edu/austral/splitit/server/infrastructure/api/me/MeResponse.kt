package edu.austral.splitit.server.infrastructure.api.me

import edu.austral.splitit.server.domain.model.user.User

data class MeResponse(
    val id: Long,
    val name: String,
    val email: String,
    // The email of these accounts is read-only: it is what ties them to Google.
    val signsInWithGoogle: Boolean,
) {
    companion object {
        fun of(user: User): MeResponse =
            MeResponse(
                id = requireNotNull(user.id),
                name = user.name,
                email = user.email,
                signsInWithGoogle = user.googleSubject != null,
            )
    }
}
