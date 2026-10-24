package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/invite-links/{token}")
class GuestSessionController(
    private val guestAccessService: GuestAccessService,
    private val tokenProvider: TokenProvider,
    private val sessionCookieWriter: SessionCookieWriter,
) {
    @PostMapping("/guest-session")
    fun enter(
        @PathVariable token: String,
        @Valid @RequestBody request: GuestSessionRequest,
        response: HttpServletResponse,
    ): GuestSessionResponse {
        val guest = guestAccessService.enter(token, requireNotNull(request.memberId))

        sessionCookieWriter.writeGuest(response, tokenProvider.issueGuest(guest))
        return GuestSessionResponse.of(guest)
    }
}
