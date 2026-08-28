package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.UserNotFoundException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val userService: UserService,
) {
    fun get(userId: Long): User = userService.findById(userId) ?: throw UserNotFoundException()

    fun update(
        userId: Long,
        name: String,
        email: String,
    ): User = userService.update(userId, name, email)
}
