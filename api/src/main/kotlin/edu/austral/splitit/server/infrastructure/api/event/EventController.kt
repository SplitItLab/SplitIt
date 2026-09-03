package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.service.CreateEventCommand
import edu.austral.splitit.server.application.service.EventApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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
}
