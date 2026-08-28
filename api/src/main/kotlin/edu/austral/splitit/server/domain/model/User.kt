package edu.austral.splitit.server.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_email", columnNames = ["email"]),
    ],
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    @field:Size(min = NAME_MIN, max = NAME_MAX)
    var name: String,
    @Column(nullable = false)
    var email: String,
    @Column(nullable = false)
    var passwordHash: String,
) {
    companion object {
        const val NAME_MIN = 2
        const val NAME_MAX = 50

        const val PASSWORD_MIN = 8
        const val PASSWORD_MAX = 100

        fun create(
            name: String,
            email: String,
            passwordHash: String,
        ): User {
            val normalizedName = name.trim()

            require(normalizedName.length in NAME_MIN..NAME_MAX) {
                "El nombre debe tener entre $NAME_MIN y $NAME_MAX caracteres"
            }

            return User(
                name = normalizedName,
                email = email.lowercase().trim(),
                passwordHash = passwordHash,
            )
        }
    }
}
