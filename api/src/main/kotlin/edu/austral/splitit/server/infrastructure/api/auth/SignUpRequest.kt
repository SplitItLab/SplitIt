package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.domain.model.User.Companion.NAME_MAX
import edu.austral.splitit.server.domain.model.User.Companion.NAME_MIN
import edu.austral.splitit.server.domain.model.User.Companion.PASSWORD_MAX
import edu.austral.splitit.server.domain.model.User.Companion.PASSWORD_MIN
import jakarta.validation.constraints.AssertTrue
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
    @field:Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
    val password: String,
) {
    @AssertTrue
    @Suppress("UnusedPrivateMember")
    private fun isTrimmedNameValid(): Boolean = name.trim().length in NAME_MIN..NAME_MAX

    override fun toString(): String = "SignUpRequest(name=$name, email=$email)"
}
