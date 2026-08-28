package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.domain.model.User
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun save(
        name: String,
        email: String,
        passwordHash: String,
    ): User {
        val newUser =
            User.create(
                name = name,
                email = email,
                passwordHash = passwordHash,
            )

        return saveNewUser(newUser)
    }

    fun findByEmail(email: String): User? = userRepository.findByEmail(email)

    private fun saveNewUser(user: User): User =
        try {
            userRepository.save(user)
        } catch (exception: DataIntegrityViolationException) {
            if (!isDuplicateEmailConstraint(exception)) {
                throw exception
            }

            throw EmailAlreadyInUseException()
        }

    private fun isDuplicateEmailConstraint(exception: DataIntegrityViolationException): Boolean {
        val violation = exception.cause as? ConstraintViolationException ?: return false

        return violation.kind == ConstraintViolationException.ConstraintKind.UNIQUE &&
            violation.constraintName
                .equals(EMAIL_UNIQUE_CONSTRAINT, ignoreCase = true)
    }

    private companion object {
        const val EMAIL_UNIQUE_CONSTRAINT = "uk_users_email"
    }
}
