package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.service.ExistingAccountChoice
import jakarta.validation.constraints.NotBlank

data class GoogleLoginRequest(
    @field:NotBlank
    val credential: String,
    val existingAccount: ExistingAccountChoice? = null,
) {
    override fun toString(): String = "GoogleLoginRequest(existingAccount=$existingAccount)"
}
