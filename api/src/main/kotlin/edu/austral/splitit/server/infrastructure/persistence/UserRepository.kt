package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean

    fun existsByEmailAndIdNot(
        email: String,
        id: Long,
    ): Boolean

    fun findByEmail(email: String): User?
}
