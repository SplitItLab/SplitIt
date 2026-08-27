package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.service.SignUpService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val signUpService: SignUpService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: SignUpRequest,
    ): SignUpResponse {
        val user =
            signUpService.register(
                name = request.name,
                email = request.email,
                password = request.password,
            )
        return SignUpResponse.of(user)
    }
}
