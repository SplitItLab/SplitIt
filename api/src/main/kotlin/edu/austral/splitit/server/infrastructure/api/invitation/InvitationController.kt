package edu.austral.splitit.server.infrastructure.api.invitation

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.service.InvitationApplicationService
import edu.austral.splitit.server.application.service.LinkMemberCommand
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/invitations")
class InvitationController(
    private val invitationApplicationService: InvitationApplicationService,
) {
    @GetMapping("/{token}")
    fun getInvitation(
        @PathVariable token: String,
    ): InvitationResponse = InvitationResponse.of(invitationApplicationService.getInvitation(token))

    @GetMapping("/{token}/members")
    fun listMembers(
        @PathVariable token: String,
    ): List<InvitationMemberResponse> {
        val members = invitationApplicationService.listMembers(token)
        return members.map { InvitationMemberResponse.of(it) }
    }

    @GetMapping("/{token}/access")
    fun getAccess(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable token: String,
    ): InvitationAccessResponse = InvitationAccessResponse.of(invitationApplicationService.getAccess(user.id, token))

    @PostMapping("/{token}/link")
    fun linkMember(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable token: String,
        @Valid @RequestBody request: LinkMemberRequest,
    ): InvitationAccessResponse {
        val command =
            LinkMemberCommand(
                userId = user.id,
                token = token,
                memberId = request.memberId,
            )

        return InvitationAccessResponse.of(invitationApplicationService.linkMember(command))
    }
}
