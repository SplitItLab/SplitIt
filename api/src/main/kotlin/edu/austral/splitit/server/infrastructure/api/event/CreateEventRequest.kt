package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.domain.model.event.Event.Companion.CURRENCY_LENGTH
import edu.austral.splitit.server.domain.model.event.Event.Companion.ICON_KEY_MAX
import edu.austral.splitit.server.domain.model.event.Event.Companion.NAME_MAX
import edu.austral.splitit.server.domain.model.event.Event.Companion.NAME_MIN
import edu.austral.splitit.server.infrastructure.api.auth.TrimmedSize
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateEventRequest(
    @field:NotBlank
    @field:TrimmedSize(min = NAME_MIN, max = NAME_MAX, allowSurroundingWhitespace = true)
    val name: String,
    val description: String? = null,
    @field:Size(max = ICON_KEY_MAX)
    val iconKey: String? = null,
    @field:NotBlank
    @field:TrimmedSize(min = CURRENCY_LENGTH, max = CURRENCY_LENGTH)
    val baseCurrency: String,
    val participantNames: List<String> = emptyList(),
)
