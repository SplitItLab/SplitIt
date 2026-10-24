package edu.austral.splitit.server.infrastructure.api.auth

import jakarta.validation.constraints.NotNull

data class GuestSessionRequest(
    @field:NotNull
    val memberId: Long?,
)
