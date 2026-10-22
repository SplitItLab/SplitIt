package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.service.KpiRunStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaLastRun::class)
@TestPropertySource(
    properties = [
        "POSTGRES_HOST=localhost",
        "POSTGRES_USER=test",
        "POSTGRES_PASSWORD=test",
        "JWT_SECRET=test-jwt-secret-that-is-long-enough",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
    ],
)
class JpaLastRunTest(
    @Autowired private val lastRun: JpaLastRun,
    @Autowired private val runs: KpiRunJpaRepository,
) {
    @Test
    fun `only a sent run moves the last successful run`() {
        assertNull(lastRun.lastSuccessfulRun())

        lastRun.recordRun(DAY, FIRST, KpiRunStatus.SENT)
        lastRun.recordRun(DAY, SECOND, KpiRunStatus.FAILED)
        lastRun.recordRun(DAY, THIRD, KpiRunStatus.SKIPPED)
        assertEquals(FIRST, lastRun.lastSuccessfulRun())

        lastRun.recordRun(DAY.plusDays(1), FOURTH, KpiRunStatus.SENT)
        assertEquals(FOURTH, lastRun.lastSuccessfulRun())
    }

    @Test
    fun `every run leaves a row with its day and status`() {
        lastRun.recordRun(DAY, FIRST, KpiRunStatus.SENT)
        lastRun.recordRun(DAY, SECOND, KpiRunStatus.FAILED)
        lastRun.recordRun(DAY.plusDays(1), THIRD, KpiRunStatus.SKIPPED)

        assertEquals(
            listOf(
                Triple(DAY, FIRST, "sent"),
                Triple(DAY, SECOND, "failed"),
                Triple(DAY.plusDays(1), THIRD, "skipped"),
            ),
            runs.findAll().sortedBy { it.id }.map { Triple(it.reportDay, it.ranAt, it.status) },
        )
    }

    @Test
    fun `a new instance reads the last successful run from the database`() {
        lastRun.recordRun(DAY, FIRST, KpiRunStatus.SENT)

        assertEquals(FIRST, JpaLastRun(runs).lastSuccessfulRun())
    }

    private companion object {
        val DAY: LocalDate = LocalDate.parse("2026-10-10")
        val FIRST: Instant = Instant.parse("2026-10-11T03:15:00Z")
        val SECOND: Instant = Instant.parse("2026-10-12T03:15:00Z")
        val THIRD: Instant = Instant.parse("2026-10-13T03:15:00Z")
        val FOURTH: Instant = Instant.parse("2026-10-14T03:15:00Z")
    }
}
