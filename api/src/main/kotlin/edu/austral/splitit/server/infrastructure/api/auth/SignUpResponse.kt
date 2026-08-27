package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.domain.model.User

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
)

data class SignUpResponse(
    val user: UserResponse,
) {
    companion object {
        fun of(user: User): SignUpResponse =
            SignUpResponse(
                user =
                    UserResponse(
                        id = requireNotNull(user.id),
                        name = user.name,
                        email = user.email,
                    ),
            )
    }
}
