package edu.austral.splitit.server.infrastructure.api.invitation

import com.fasterxml.jackson.annotation.JsonProperty
import edu.austral.splitit.server.application.service.InvitationAccess
import edu.austral.splitit.server.application.service.InvitationMemberSummary
import edu.austral.splitit.server.application.service.InvitationSummary
import edu.austral.splitit.server.application.service.LinkedMemberSummary

data class InvitationResponse(
    val name: String,
    val description: String?,
    val iconKey: String?,
    val memberCount: Long,
) {
    companion object {
        fun of(summary: InvitationSummary): InvitationResponse =
            InvitationResponse(
                name = summary.name,
                description = summary.description,
                iconKey = summary.iconKey,
                memberCount = summary.memberCount,
            )
    }
}

data class InvitationMemberResponse(
    val id: Long,
    val name: String,
    @get:JsonProperty("isLinked")
    val isLinked: Boolean,
) {
    companion object {
        fun of(member: InvitationMemberSummary): InvitationMemberResponse =
            InvitationMemberResponse(
                id = member.id,
                name = member.name,
                isLinked = member.isLinked,
            )
    }
}

data class InvitationAccessResponse(
    val eventId: Long,
    val linkedMember: LinkedMemberResponse?,
) {
    companion object {
        fun of(access: InvitationAccess): InvitationAccessResponse =
            InvitationAccessResponse(
                eventId = access.eventId,
                linkedMember = access.linkedMember?.let { LinkedMemberResponse.of(it) },
            )
    }
}

data class LinkedMemberResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun of(member: LinkedMemberSummary): LinkedMemberResponse =
            LinkedMemberResponse(
                id = member.id,
                name = member.name,
            )
    }
}
