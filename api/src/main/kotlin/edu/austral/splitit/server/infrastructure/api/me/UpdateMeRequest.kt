package edu.austral.splitit.server.infrastructure.api.me

import edu.austral.splitit.server.domain.model.user.User.Companion.NAME_MAX
import edu.austral.splitit.server.domain.model.user.User.Companion.NAME_MIN
import edu.austral.splitit.server.infrastructure.api.auth.TrimmedSize
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateMeRequest(
    @field:NotBlank
    @field:TrimmedSize(min = NAME_MIN, max = NAME_MAX, allowSurroundingWhitespace = true)
    val name: String,
    @field:NotBlank
    @field:Email
    val email: String,
) {
    override fun toString(): String = "UpdateMeRequest(name=$name, email=$email)"
}
