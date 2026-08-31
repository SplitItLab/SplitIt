package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val userService: UserService,
) {
    fun get(userId: Long): User = userService.getById(userId)

    fun update(
        userId: Long,
        name: String,
        email: Email,
    ): User = userService.update(userId, name, email)
}
