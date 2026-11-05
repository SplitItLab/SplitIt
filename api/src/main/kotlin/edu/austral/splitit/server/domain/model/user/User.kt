package edu.austral.splitit.server.domain.model.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import java.time.Instant

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_users_google_subject", columnNames = ["google_subject"]),
    ],
)
class User private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    @field:Size(min = NAME_MIN, max = NAME_MAX)
    var name: String,
    @Column(nullable = false)
    var email: String,
    // Null for accounts that sign in with Google.
    @Column
    var passwordHash: String?,
    @Column(name = "google_subject")
    var googleSubject: String? = null,
    @Column(name = "disabled_at")
    var disabledAt: Instant? = null,
) {
    companion object {
        const val NAME_MIN = 2
        const val NAME_MAX = 50

        const val PASSWORD_MIN = 8
        const val PASSWORD_MAX = 100

        private const val DISABLED_EMAIL_DOMAIN = "splitit.invalid"

        fun create(
            name: String,
            email: Email,
            passwordHash: String,
        ): User =
            User(
                name = normalizeName(name),
                email = email.value,
                passwordHash = passwordHash,
            )

        fun createWithGoogle(
            name: String,
            email: Email,
            googleSubject: String,
        ): User =
            User(
                name = normalizeName(name),
                email = email.value,
                passwordHash = null,
                googleSubject = googleSubject,
            )

        private fun normalizeName(name: String): String {
            val normalizedName = name.trim()

            require(normalizedName.length in NAME_MIN..NAME_MAX) {
                "Name must be between $NAME_MIN and $NAME_MAX characters"
            }

            return normalizedName
        }
    }

    fun updateProfile(
        name: String,
        email: Email,
    ) {
        // The email is what ties the account to Google, so it can never change.
        if (googleSubject != null && email.value != this.email) throw GoogleAccountEmailLockedException()

        this.name = normalizeName(name)
        this.email = email.value
    }

    fun linkGoogle(googleSubject: String) {
        this.googleSubject = googleSubject
        this.passwordHash = null
    }

    // Frees the email for the account that replaces this one.
    fun disable() {
        this.email = "deleted-${requireNotNull(id)}@$DISABLED_EMAIL_DOMAIN"
        this.passwordHash = null
        this.googleSubject = null
        this.disabledAt = Instant.now()
    }
}
