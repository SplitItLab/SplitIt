package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.validateBetween

@ConsistentCopyVisibility
data class EventName private constructor(
    val name: String,
) {
    fun get(): String = name

    companion object {
        const val EVENT_NAME_MIN = 3
        const val EVENT_NAME_MAX = 100

        operator fun invoke(rawName: String): EventName {
            val normalized = rawName.trim()

            validateBetween("event name", normalized, EVENT_NAME_MIN, EVENT_NAME_MAX)

            return EventName(normalized)
        }
    }
}
