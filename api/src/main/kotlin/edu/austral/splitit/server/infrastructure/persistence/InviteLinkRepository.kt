package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.event.InviteLink
import org.springframework.data.jpa.repository.JpaRepository

interface InviteLinkRepository : JpaRepository<InviteLink, Long> {
    fun findByEventId(eventId: Long): InviteLink?

    fun findByToken(token: String): InviteLink?
}
