package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.service.EventSummary

data class EventResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
) {
    companion object {
        fun of(summary: EventSummary): EventResponse =
            EventResponse(
                id = summary.id,
                name = summary.name,
                description = summary.description,
                iconKey = summary.iconKey,
                baseCurrency = summary.baseCurrency,
                memberCount = summary.memberCount,
            )
    }
}
