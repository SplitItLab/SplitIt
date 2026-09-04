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
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "event_members",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_members_event_user", columnNames = ["event_id", "user_id"]),
    ],
    indexes = [
        Index(name = "idx_event_members_event_id", columnList = "event_id"),
        Index(name = "idx_event_members_user_id", columnList = "user_id"),
    ],
)
class EventMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event,
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    var user: User? = null,
    @Column(name = "display_name", nullable = false, length = DISPLAY_NAME_MAX)
    var displayName: String,
    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now(),
) {
    companion object {
        const val DISPLAY_NAME_MIN = 1
        const val DISPLAY_NAME_MAX = 100

        fun create(
            event: Event,
            displayName: String,
            user: User? = null,
        ): EventMember {
            val normalizedName = displayName.trim()
            require(normalizedName.length in DISPLAY_NAME_MIN..DISPLAY_NAME_MAX) {
                "El nombre del integrante debe tener entre $DISPLAY_NAME_MIN y $DISPLAY_NAME_MAX caracteres"
            }

            return EventMember(
                event = event,
                user = user,
                displayName = normalizedName,
                joinedAt = Instant.now(),
            )
        }
    }

    fun linkUser(user: User) {
        this.user = user
    }
}
