package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.domain.model.event.Event.Companion.ICON_KEY_MAX
import edu.austral.splitit.server.domain.model.event.EventName.Companion.EVENT_NAME_MAX
import edu.austral.splitit.server.domain.model.event.EventName.Companion.EVENT_NAME_MIN
import edu.austral.splitit.server.infrastructure.api.auth.TrimmedSize
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateEventRequest(
    @field:NotBlank
    @field:TrimmedSize(min = EVENT_NAME_MIN, max = EVENT_NAME_MAX, allowSurroundingWhitespace = true)
    val name: String?,
    val description: String?,
    @field:Size(max = ICON_KEY_MAX)
    val iconKey: String?,
)
