package edu.austral.splitit.server.domain.model.user

@JvmInline
value class Email private constructor(
    val value: String,
) {
    companion object {
        fun create(value: String): Result<Email> {
            val email = value.trim().lowercase()
            if (!isValid(email)) {
                return Result.failure(
                    IllegalArgumentException("Invalid email: $value"),
                )
            }

            return Result.success(Email(email))
        }

        private fun isValid(value: String): Boolean =
            value.matches(
                Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"),
            )
    }
}
