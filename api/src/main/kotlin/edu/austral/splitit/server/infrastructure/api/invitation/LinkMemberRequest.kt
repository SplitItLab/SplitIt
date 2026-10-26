package edu.austral.splitit.server.infrastructure.api.invitation

import jakarta.validation.constraints.Positive

data class LinkMemberRequest(
    @field:Positive
    val memberId: Long,
)
