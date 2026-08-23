package edu.austral.splitit.server.infrastructure.api.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8)
    val password: String,
) {
    override fun toString(): String = "SignUpRequest(name=$name, email=$email)"
}
