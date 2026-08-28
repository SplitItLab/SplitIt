package edu.austral.splitit.server.infrastructure.api.me

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.AccountService
import edu.austral.splitit.server.infrastructure.api.auth.UserResponse
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me")
class MeController(
    private val accountService: AccountService,
    private val tokenProvider: TokenProvider,
    private val sessionCookieWriter: SessionCookieWriter,
) {
    @GetMapping
    fun get(
        @AuthenticationPrincipal user: AuthUser,
    ): UserResponse = UserResponse.of(accountService.get(user.id))

    @PatchMapping
    fun update(
        @AuthenticationPrincipal user: AuthUser,
        @Valid @RequestBody request: UpdateMeRequest,
        response: HttpServletResponse,
    ): UserResponse {
        val updated =
            accountService.update(
                userId = user.id,
                name = request.name,
                email = request.email,
            )
        val token =
            tokenProvider.issue(
                AuthUser(
                    id = requireNotNull(updated.id),
                    username = updated.email,
                    password = "",
                    roles = user.roles,
                    name = updated.name,
                ),
            )
        sessionCookieWriter.write(response, token)
        return UserResponse.of(updated)
    }
}
