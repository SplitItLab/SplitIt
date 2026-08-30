package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import edu.austral.splitit.server.application.exception.UserNotFoundException
import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun save(
        name: String,
        email: Email,
        passwordHash: String,
    ): User {
        val user =
            User.create(
                name = name,
                email = email,
                passwordHash = passwordHash,
            )

        return persist(user)
    }

    fun update(
        id: Long,
        name: String,
        email: Email,
    ): User {
        val user = getById(id)

        if (userRepository.existsByEmailAndIdNot(email.value, id)) {
            throw EmailAlreadyInUseException()
        }

        user.updateProfile(name, email)

        return persist(user)
    }

    private fun persist(user: User): User =
        try {
            userRepository.saveAndFlush(user)
        } catch (exception: DataIntegrityViolationException) {
            if (!isDuplicateEmailConstraint(exception)) {
                throw exception
            }

            throw EmailAlreadyInUseException()
        }

    private fun isDuplicateEmailConstraint(exception: DataIntegrityViolationException): Boolean {
        val violation = exception.cause as? ConstraintViolationException ?: return false

        return violation.kind == ConstraintViolationException.ConstraintKind.UNIQUE &&
            EMAIL_UNIQUE_CONSTRAINT.equals(violation.constraintName, ignoreCase = true)
    }

    fun findById(id: Long): User? = userRepository.findByIdOrNull(id)

    fun findByEmail(email: Email): User? = userRepository.findByEmail(email.value)

    fun getById(id: Long): User = findById(id) ?: throw UserNotFoundException()

    private companion object {
        const val EMAIL_UNIQUE_CONSTRAINT = "uk_users_email"
    }
}
