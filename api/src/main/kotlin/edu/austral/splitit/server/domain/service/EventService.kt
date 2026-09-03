package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class EventService(
    private val eventRepository: EventRepository,
) {
    fun save(
        owner: User,
        name: String,
        description: String? = null,
        iconKey: String? = null,
        baseCurrency: String,
    ): Event {
        val event =
            Event.create(
                owner = owner,
                name = name,
                description = description,
                iconKey = iconKey,
                baseCurrency = baseCurrency,
            )
        return eventRepository.save(event)
    }

    fun save(event: Event): Event = eventRepository.save(event)

    fun findById(id: Long): Event? = eventRepository.findByIdOrNull(id)

    fun getById(id: Long): Event = findById(id) ?: throw EventNotFoundException()

    fun findUserEvents(userId: Long): List<Event> = eventRepository.findDistinctByOwnerIdOrMemberUserId(userId)
}
