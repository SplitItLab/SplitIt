package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.validateBetween

@ConsistentCopyVisibility
data class EventDescription private constructor(
    val description: String,
) {
    fun get(): String = description

    companion object {
        const val EVENT_DESCRIPTION_MAX = 500

        operator fun invoke(rawDescription: String): EventDescription {
            val normalized = rawDescription.trim()

            validateBetween("event description", normalized, 0, EVENT_DESCRIPTION_MAX)

            return EventDescription(normalized)
        }
    }
}
