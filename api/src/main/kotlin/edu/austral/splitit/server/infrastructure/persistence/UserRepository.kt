package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmailAndIdNot(
        email: String,
        id: Long,
    ): Boolean

    fun findByEmail(email: String): User?
}
