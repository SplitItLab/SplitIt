package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.domain.model.User

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
) {
    companion object {
        fun of(user: User): UserResponse =
            UserResponse(
                id = requireNotNull(user.id),
                name = user.name,
                email = user.email,
            )

        fun of(authUser: AuthUser): UserResponse =
            UserResponse(
                id = authUser.id,
                name = authUser.name,
                email = authUser.username,
            )
    }
}

data class SignUpResponse(
    val user: UserResponse,
) {
    companion object {
        fun of(user: User): SignUpResponse = SignUpResponse(user = UserResponse.of(user))
    }
}
