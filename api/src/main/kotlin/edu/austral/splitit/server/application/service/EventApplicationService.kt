package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EventDeletionConflictException
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateEventCommand(
    val userId: Long,
    val name: String,
    val description: String? = null,
    val iconKey: String? = null,
    val baseCurrency: String,
    val participantNames: List<String> = emptyList(),
)

data class UpdateEventCommand(
    val userId: Long,
    val eventId: Long,
    val name: String? = null,
    val description: String? = null,
    val iconKey: String? = null,
)

data class EventSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
)

data class EventDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
    val members: List<EventMemberSummary>,
    val isOwner: Boolean,
)

data class EventMemberSummary(
    val id: Long,
    val name: String,
    val email: String?,
    val isGuest: Boolean,
)

@Service
class EventApplicationService(
    private val userService: UserService,
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
    private val inviteLinkService: InviteLinkService,
) {
    @Transactional
    fun createEvent(command: CreateEventCommand): EventSummary {
        val owner = userService.getById(command.userId)

        val normalizedAdditionalNames =
            command.participantNames.map { name ->
                val trimmed = name.trim()
                require(trimmed.isNotEmpty()) {
                    "Participant name cannot be empty"
                }
                trimmed
            }

        val ownerKey = owner.name.trim().lowercase()
        val additionalKeys = normalizedAdditionalNames.map { it.lowercase() }
        require(additionalKeys.toSet().size == additionalKeys.size) {
            "Duplicate participant names"
        }
        require(ownerKey !in additionalKeys) {
            "Participant name cannot match the event owner name"
        }

        val event =
            eventService.save(
                owner = owner,
                name = command.name,
                description = command.description,
                iconKey = command.iconKey,
                baseCurrency = command.baseCurrency,
            )

        eventMemberService.addMember(
            event = event,
            displayName = owner.name,
            user = owner,
        )

        if (normalizedAdditionalNames.isNotEmpty()) {
            eventMemberService.addMembers(event, normalizedAdditionalNames)
        }

        val totalMembers = 1L + normalizedAdditionalNames.size

        return EventSummary(
            id = requireNotNull(event.id),
            name = event.name,
            description = event.description,
            iconKey = event.iconKey,
            baseCurrency = event.baseCurrency,
            memberCount = totalMembers,
        )
    }

    @Transactional(readOnly = true)
    fun listUserEvents(userId: Long): List<EventSummary> {
        val events = eventService.findUserEvents(userId)
        if (events.isEmpty()) return emptyList()

        val eventIds = events.mapNotNull { it.id }
        val counts = eventMemberService.getMemberCounts(eventIds)

        return events.map { event ->
            EventSummary(
                id = requireNotNull(event.id),
                name = event.name,
                description = event.description,
                iconKey = event.iconKey,
                baseCurrency = event.baseCurrency,
                memberCount = counts[event.id] ?: 0L,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getEventById(
        userId: Long,
        eventId: Long,
    ): EventDetail {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        val isOwner = event.owner.id == userId
        val isMember = isOwner || eventMemberService.isUserMemberOfEvent(eventId, userId)

        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        val members = eventMemberService.findByEventId(eventId)

        return EventDetail(
            id = requireNotNull(event.id),
            name = event.name,
            description = event.description,
            iconKey = event.iconKey,
            baseCurrency = event.baseCurrency,
            memberCount = members.size.toLong(),
            members =
                members.map { member ->
                    EventMemberSummary(
                        id = requireNotNull(member.id),
                        name = member.displayName,
                        email = member.user?.email,
                        isGuest = member.user == null,
                    )
                },
            isOwner = isOwner,
        )
    }

    @Transactional
    fun getOrCreateInviteToken(
        userId: Long,
        eventId: Long,
    ): String {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        if (event.owner.id != userId) {
            throw AccessDeniedException("Forbidden")
        }

        return inviteLinkService.getOrCreate(event).token
    }

    @Transactional
    fun updateEvent(command: UpdateEventCommand): EventSummary {
        val event =
            eventService.findById(command.eventId)
                ?: throw EventNotFoundException()

        require(event.owner.id == command.userId) {
            throw AccessDeniedException("Forbidden")
        }

        val updated =
            eventService.update(
                event = event,
                name = command.name,
                description = command.description,
                iconKey = command.iconKey,
            )

        val members = eventMemberService.findByEventId(command.eventId)

        return EventSummary(
            id = requireNotNull(updated.id),
            name = updated.name,
            description = updated.description,
            iconKey = updated.iconKey,
            baseCurrency = updated.baseCurrency,
            memberCount = members.size.toLong(),
        )
    }

    @Transactional
    fun deleteEvent(
        userId: Long,
        eventId: Long,
    ) {
        val event = eventService.getById(eventId)

        require(event.owner.id == userId) {
            throw AccessDeniedException("Forbidden")
        }

        ensureDeletable(eventId)

        try {
            eventMemberService.deleteByEventId(eventId)
            eventService.delete(event)
        } catch (exception: DataIntegrityViolationException) {
            throw EventDeletionConflictException(cause = exception)
        }
    }

    private fun ensureDeletable(eventId: Long) {
        if (eventService.hasExpenses(eventId)) {
            throw EventDeletionConflictException()
        }
    }
}
