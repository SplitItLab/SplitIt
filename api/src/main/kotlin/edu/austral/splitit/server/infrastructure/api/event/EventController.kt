package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.service.CreateEventCommand
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.application.service.UpdateEventCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventApplicationService: EventApplicationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthUser,
        @Valid @RequestBody request: CreateEventRequest,
    ): EventResponse {
        val command =
            CreateEventCommand(
                userId = user.id,
                name = request.name,
                description = request.description,
                iconKey = request.iconKey,
                baseCurrency = request.baseCurrency,
                participantNames = request.participantNames,
            )

        val summary = eventApplicationService.createEvent(command)

        return EventResponse.of(summary)
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal user: AuthUser,
    ): List<EventResponse> {
        val summaries = eventApplicationService.listUserEvents(user.id)

        return summaries.map { EventResponse.of(it) }
    }

    @GetMapping("/{id}")
    fun getEventById(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable id: Long,
    ): EventDetailResponse {
        val detail = eventApplicationService.getEventById(user.id, id)
        return EventDetailResponse.of(detail)
    }

    @PutMapping("/{id}")
    fun updateEvent(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateEventRequest,
    ): EventResponse {
        val command =
            UpdateEventCommand(
                userId = user.id,
                eventId = id,
                name = request.name,
                description = request.description,
                iconKey = request.iconKey,
            )

        val event = eventApplicationService.updateEvent(command)
        return EventResponse.of(event)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable id: Long,
    ) {
        eventApplicationService.deleteEvent(user.id, id)
    }
}
