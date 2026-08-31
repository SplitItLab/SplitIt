package edu.austral.splitit.server.infrastructure.api.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
) {
    override fun toString(): String = "LoginRequest(email=$email)"
}
