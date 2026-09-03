package edu.austral.splitit.server.domain.model.user

@JvmInline
value class Email private constructor(
    val value: String,
) {
    companion object {
        private val LOCAL_PART_PATTERN = Regex("^[A-Za-z0-9+_.-]+$")

        fun create(value: String): Result<Email> {
            val email = value.trim().lowercase()
            if (!isValid(email)) {
                return Result.failure(
                    IllegalArgumentException("Invalid email: $value"),
                )
            }

            return Result.success(Email(email))
        }

        private fun isValid(value: String): Boolean {
            val separator = value.indexOf('@')
            if (separator <= 0 || separator != value.lastIndexOf('@')) {
                return false
            }

            val local = value.substring(0, separator)
            val domain = value.substring(separator + 1)
            return LOCAL_PART_PATTERN.matches(local) && isValidDomain(domain)
        }

        private fun isValidDomain(domain: String): Boolean {
            if (domain.isEmpty()) {
                return false
            }

            return domain.split('.').all(::isValidLabel)
        }

        private fun isValidLabel(label: String): Boolean {
            if (label.isEmpty() || label.startsWith('-') || label.endsWith('-')) {
                return false
            }

            return label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        }
    }
}
