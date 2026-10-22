package edu.austral.splitit.server.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface KpiRunJpaRepository : JpaRepository<KpiRunEntity, Long> {
    @Query("SELECT MAX(r.ranAt) FROM KpiRunEntity r WHERE r.status = :status")
    fun findLastRanAt(
        @Param("status") status: String,
    ): Instant?
}
