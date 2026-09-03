package edu.austral.splitit.server.domain.model.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "invite_links",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_invite_links_event_id", columnNames = ["event_id"]),
        UniqueConstraint(name = "uk_invite_links_token", columnNames = ["token"]),
    ],
)
class InviteLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    var event: Event,
    @Column(nullable = false, unique = true, length = TOKEN_MAX_LENGTH)
    var token: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
) {
    companion object {
        const val TOKEN_MAX_LENGTH = 128

        fun create(
            event: Event,
            token: String,
        ): InviteLink {
            require(token.isNotBlank()) { "El token de invitación no puede estar vacío" }
            return InviteLink(
                event = event,
                token = token,
                createdAt = Instant.now(),
            )
        }
    }
}
