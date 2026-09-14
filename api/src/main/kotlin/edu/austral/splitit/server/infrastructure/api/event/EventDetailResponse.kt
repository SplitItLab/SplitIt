package edu.austral.splitit.server.infrastructure.api.event

import com.fasterxml.jackson.annotation.JsonProperty
import edu.austral.splitit.server.application.service.EventDetail
import edu.austral.splitit.server.application.service.EventMemberSummary

data class EventDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
    val members: List<EventMemberResponse>,
) {
    companion object {
        fun of(detail: EventDetail): EventDetailResponse =
            EventDetailResponse(
                id = detail.id,
                name = detail.name,
                description = detail.description,
                iconKey = detail.iconKey,
                baseCurrency = detail.baseCurrency,
                memberCount = detail.memberCount,
                members = detail.members.map { EventMemberResponse.of(it) },
            )
    }
}

data class EventMemberResponse(
    val id: Long,
    val name: String,
    val email: String?,
    @get:JsonProperty("isGuest")
    val isGuest: Boolean,
) {
    companion object {
        fun of(member: EventMemberSummary): EventMemberResponse =
            EventMemberResponse(
                id = member.id,
                name = member.name,
                email = member.email,
                isGuest = member.isGuest,
            )
    }
}
