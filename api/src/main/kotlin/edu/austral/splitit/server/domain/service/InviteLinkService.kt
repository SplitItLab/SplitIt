package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.InviteLink
import edu.austral.splitit.server.infrastructure.persistence.InviteLinkRepository
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64

@Service
class InviteLinkService(
    private val inviteLinkRepository: InviteLinkRepository,
) {
    private val random = SecureRandom()

    fun getOrCreate(event: Event): InviteLink =
        inviteLinkRepository.findByEventId(requireNotNull(event.id))
            ?: inviteLinkRepository.save(InviteLink.create(event, generateToken()))

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
