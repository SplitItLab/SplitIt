package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_events_owner_id", columnList = "owner_id"),
    ],
)
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,
    @Column(nullable = false, length = NAME_MAX)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "icon_key", length = ICON_KEY_MAX)
    var iconKey: String? = null,
    @Column(name = "base_currency", nullable = false, length = CURRENCY_LENGTH)
    var baseCurrency: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    companion object {
        const val NAME_MIN = 1
        const val NAME_MAX = 100
        const val ICON_KEY_MAX = 50
        const val CURRENCY_LENGTH = 3

        fun create(
            owner: User,
            name: String,
            description: String? = null,
            iconKey: String? = null,
            baseCurrency: String,
        ): Event {
            val normalizedName = name.trim()
            require(normalizedName.length in NAME_MIN..NAME_MAX) {
                "El nombre del evento debe tener entre $NAME_MIN y $NAME_MAX caracteres"
            }

            val normalizedCurrency = baseCurrency.trim().uppercase()
            require(normalizedCurrency.length == CURRENCY_LENGTH) {
                "La moneda base debe ser un código ISO 4217 de $CURRENCY_LENGTH caracteres"
            }

            val now = Instant.now()
            return Event(
                owner = owner,
                name = normalizedName,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                iconKey = iconKey?.trim()?.takeIf { it.isNotEmpty() },
                baseCurrency = normalizedCurrency,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
