package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.exception.InvalidRequestException
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.infrastructure.api.ErrorMessage
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val signUpService: SignUpService,
    private val loginService: LoginService,
    private val tokenProvider: TokenProvider,
    private val sessionCookieWriter: SessionCookieWriter,
    @Value("\${cors.allowed-origins}") allowedOriginsValue: String,
) {
    private val allowedOrigins =
        allowedOriginsValue
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: SignUpRequest,
    ): UserResponse {
        val email =
            Email.create(request.email).getOrNull()
                ?: throw InvalidRequestException()

        val user =
            signUpService.register(
                name = request.name,
                email = email,
                password = request.password,
            )

        return UserResponse.of(user)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse,
    ): UserResponse {
        val email =
            Email.create(request.email).getOrNull()
                ?: throw InvalidRequestException()

        val user = loginService.login(email, request.password)

        val token =
            tokenProvider.issue(
                AuthUser(
                    id = requireNotNull(user.id),
                    username = user.email,
                    password = "",
                    roles = emptyList(),
                    name = user.name,
                ),
            )
        sessionCookieWriter.write(response, token)
        return UserResponse.of(user)
    }

    @GetMapping("/session")
    fun session(
        @AuthenticationPrincipal user: AuthUser,
    ): UserResponse = UserResponse.of(user)

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ErrorMessage> {
        if (!isTrustedOrigin(request)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorMessage("Forbidden"))
        }

        sessionCookieWriter.clear(response)
        return ResponseEntity.noContent().build()
    }

    private fun isTrustedOrigin(request: HttpServletRequest): Boolean {
        val origin = request.getHeader(HttpHeaders.ORIGIN)?.trim().orEmpty()
        val referer = request.getHeader(HttpHeaders.REFERER)?.trim().orEmpty()
        return when {
            origin.isNotEmpty() -> origin in allowedOrigins
            referer.isNotEmpty() -> originFromReferer(referer) in allowedOrigins
            else -> true
        }
    }

    private fun originFromReferer(referer: String): String? {
        val uri = runCatching { URI(referer) }.getOrNull()
        val scheme = uri?.scheme
        val authority = uri?.authority
        return if (scheme != null && authority != null) "$scheme://$authority" else null
    }
}
