package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.InviteLinkNotFoundException
import edu.austral.splitit.server.application.exception.MemberHasAccountException
import edu.austral.splitit.server.application.exception.MemberNotInEventException
import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.InviteLinkService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GuestAccessService(
    private val inviteLinkService: InviteLinkService,
    private val eventMemberService: EventMemberService,
) {
    @Transactional(readOnly = true)
    fun enter(
        inviteToken: String,
        memberId: Long,
    ): AuthGuest {
        val inviteLink =
            inviteLinkService.findByToken(inviteToken)
                ?: throw InviteLinkNotFoundException()
        val eventId = requireNotNull(inviteLink.event.id)
        val member = findGuestMember(eventId, memberId)

        return AuthGuest(
            eventId = eventId,
            memberId = memberId,
            name = member.displayName,
        )
    }

    private fun findGuestMember(
        eventId: Long,
        memberId: Long,
    ): EventMember {
        val member =
            eventMemberService
                .findById(memberId)
                ?.takeIf { it.event.id == eventId }
                ?: throw MemberNotInEventException()

        // Choosing a member with an account requires logging in as that account.
        if (member.user != null) {
            throw MemberHasAccountException()
        }
        return member
    }
}
