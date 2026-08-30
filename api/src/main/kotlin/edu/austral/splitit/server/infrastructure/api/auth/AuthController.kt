package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.LoginService
import edu.austral.splitit.server.application.service.SignUpService
import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val signUpService: SignUpService,
    private val loginService: LoginService,
    private val tokenProvider: TokenProvider,
    private val sessionCookieWriter: SessionCookieWriter,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: SignUpRequest,
    ): UserResponse {
        val email =
            Email.create(request.email).getOrNull()
                ?: throw IllegalArgumentException("Invalid email format")

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
                ?: throw IllegalArgumentException("Invalid email format")

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
}
