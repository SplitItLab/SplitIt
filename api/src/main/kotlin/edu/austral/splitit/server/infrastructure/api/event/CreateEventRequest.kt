package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.domain.model.event.Currency.Companion.CURRENCY_LENGTH
import edu.austral.splitit.server.domain.model.event.Event.Companion.ICON_KEY_MAX
import edu.austral.splitit.server.domain.model.event.EventMember.Companion.DISPLAY_NAME_MAX
import edu.austral.splitit.server.domain.model.event.EventMember.Companion.DISPLAY_NAME_MIN
import edu.austral.splitit.server.domain.model.event.EventName.Companion.EVENT_NAME_MAX
import edu.austral.splitit.server.domain.model.event.EventName.Companion.EVENT_NAME_MIN
import edu.austral.splitit.server.infrastructure.api.auth.TrimmedSize
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateEventRequest(
    @field:NotBlank
    @field:TrimmedSize(min = EVENT_NAME_MIN, max = EVENT_NAME_MAX, allowSurroundingWhitespace = true)
    val name: String,
    val description: String? = null,
    @field:Size(max = ICON_KEY_MAX)
    val iconKey: String? = null,
    @field:NotBlank
    @field:TrimmedSize(min = CURRENCY_LENGTH, max = CURRENCY_LENGTH)
    @field:Pattern(regexp = "^[A-Za-z]{3}$")
    val baseCurrency: String,
    val participantNames: List<
        @NotBlank
        @TrimmedSize(
            min = DISPLAY_NAME_MIN,
            max = DISPLAY_NAME_MAX,
            allowSurroundingWhitespace = true,
        )
        String,
    > = emptyList(),
)
