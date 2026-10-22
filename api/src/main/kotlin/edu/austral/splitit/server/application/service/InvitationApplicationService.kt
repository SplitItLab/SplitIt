package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.AccountAlreadyLinkedException
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class InvitationSummary(
    val name: String,
    val description: String?,
    val iconKey: String?,
    val memberCount: Long,
)

data class InvitationMemberSummary(
    val id: Long,
    val name: String,
    val isLinked: Boolean,
)

data class LinkedMemberSummary(
    val id: Long,
    val name: String,
)

data class InvitationAccess(
    val eventId: Long,
    val linkedMember: LinkedMemberSummary?,
)

data class LinkMemberCommand(
    val userId: Long,
    val token: String,
    val memberId: Long,
)

@Service
class InvitationApplicationService(
    private val userService: UserService,
    private val eventMemberService: EventMemberService,
    private val inviteLinkService: InviteLinkService,
) {
    @Transactional(readOnly = true)
    fun getInvitation(token: String): InvitationSummary {
        val event = findEventByToken(token)

        return InvitationSummary(
            name = event.name,
            description = event.description,
            iconKey = event.iconKey,
            memberCount = eventMemberService.countByEventId(requireNotNull(event.id)),
        )
    }

    @Transactional(readOnly = true)
    fun listMembers(token: String): List<InvitationMemberSummary> {
        val event = findEventByToken(token)

        return eventMemberService.findByEventId(requireNotNull(event.id)).map { member ->
            InvitationMemberSummary(
                id = requireNotNull(member.id),
                name = member.displayName,
                isLinked = member.user != null,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAccess(
        userId: Long,
        token: String,
    ): InvitationAccess {
        val eventId = requireNotNull(findEventByToken(token).id)
        val linkedMember = eventMemberService.findByEventIdAndUserId(eventId, userId)

        return InvitationAccess(
            eventId = eventId,
            linkedMember = linkedMember?.let { toLinkedMemberSummary(it) },
        )
    }

    @Transactional
    fun linkMember(command: LinkMemberCommand): InvitationAccess {
        val eventId = requireNotNull(findEventByToken(command.token).id)

        if (eventMemberService.isUserMemberOfEvent(eventId, command.userId)) {
            throw AccountAlreadyLinkedException()
        }

        val member = eventMemberService.findById(command.memberId)
        require(member != null && member.event.id == eventId) {
            "Member does not belong to the event"
        }

        member.linkUser(userService.getById(command.userId))

        try {
            eventMemberService.saveAndFlush(member)
        } catch (exception: DataIntegrityViolationException) {
            throw AccountAlreadyLinkedException(cause = exception)
        }

        return InvitationAccess(
            eventId = eventId,
            linkedMember = toLinkedMemberSummary(member),
        )
    }

    private fun findEventByToken(token: String): Event {
        val event = inviteLinkService.findEventByToken(token)
        return event ?: throw EventNotFoundException()
    }

    private fun toLinkedMemberSummary(member: EventMember): LinkedMemberSummary =
        LinkedMemberSummary(
            id = requireNotNull(member.id),
            name = member.displayName,
        )
}
