package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.UserService
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

data class EventSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
)

@Service
class EventApplicationService(
    private val userService: UserService,
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
) {
    @Transactional
    fun createEvent(command: CreateEventCommand): EventSummary {
        val owner = userService.getById(command.userId)

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

        val validAdditionalNames =
            command.participantNames
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        if (validAdditionalNames.isNotEmpty()) {
            eventMemberService.addMembers(event, validAdditionalNames)
        }

        val totalMembers = 1L + validAdditionalNames.size

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
}
