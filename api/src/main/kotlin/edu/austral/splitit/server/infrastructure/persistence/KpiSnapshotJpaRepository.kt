package edu.austral.splitit.server.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KpiSnapshotJpaRepository : JpaRepository<KpiSnapshotEntity, Long> {
    fun findAllByChannelOrderByIdAsc(channel: String): List<KpiSnapshotEntity>
}
