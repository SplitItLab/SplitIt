package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.domain.model.User.Companion.NAME_MAX
import edu.austral.splitit.server.domain.model.User.Companion.NAME_MIN
import edu.austral.splitit.server.domain.model.User.Companion.PASSWORD_MAX
import edu.austral.splitit.server.domain.model.User.Companion.PASSWORD_MIN
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank
    @field:TrimmedSize(min = NAME_MIN, max = NAME_MAX)
    val name: String,
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
    val password: String,
) {
    override fun toString(): String = "SignUpRequest(name=$name, email=$email)"
}
