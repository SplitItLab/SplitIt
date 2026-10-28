package edu.austral.splitit.server.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RequestSampleJpaRepository : JpaRepository<RequestSampleEntity, Long> {
    // Nearest rank, the same value as percentile_disc on Postgres.
    // percentile_disc itself is not used: H2, the test database, picks a lower rank.
    @Query(
        value = """
        SELECT duration_us FROM (
            SELECT duration_us,
                   ROW_NUMBER() OVER (ORDER BY duration_us) AS sample_rank,
                   COUNT(*) OVER () AS sample_count
            FROM request_sample
            WHERE endpoint = :endpoint AND recorded_at BETWEEN :from AND :to
        ) ranked
        WHERE sample_rank = CEIL(0.95 * sample_count)
        """,
        nativeQuery = true,
    )
    fun p95DurationMicros(
        @Param("endpoint") endpoint: String,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): Int?

    @Query(
        value = """
        SELECT CAST(COUNT(CASE WHEN status_code BETWEEN 500 AND 599 THEN 1 END) AS DOUBLE PRECISION)
               / NULLIF(COUNT(*), 0)
        FROM request_sample
        WHERE recorded_at BETWEEN :from AND :to
        """,
        nativeQuery = true,
    )
    fun rate5xx(
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): Double?

    @Modifying
    @Query("DELETE FROM RequestSampleEntity s WHERE s.recordedAt < :cutoff")
    fun deleteRecordedBefore(
        @Param("cutoff") cutoff: Instant,
    )
}
