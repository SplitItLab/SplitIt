package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.kpi.KpiSnapshotExporter
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaKpiSnapshotStore::class)
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
class RunKpiReportRollbackTest(
    @Autowired private val store: JpaKpiSnapshotStore,
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    @Test
    fun `a failed run record rolls back the stored snapshot`() {
        val engine = mock<KpiEngine>()
        whenever(engine.calculate(any())).thenReturn(
            listOf(KpiResult(KpiIds.EVENTS_CREATED_PER_DAY, KpiValue.Numeric(NUMERIC), CALCULATED_AT)),
        )
        val service =
            RunKpiReport(
                engine = engine,
                exporter = KpiSnapshotExporter(store, CHANNEL),
                lastRun = FailingLastRun(),
                clock = Clock.fixed(CALCULATED_AT, ZoneOffset.UTC),
                transactions = TransactionTemplate(transactionManager),
            )

        assertFailsWith<IllegalStateException> {
            service.run(LocalDate.parse("2026-10-11"))
        }

        assertTrue(store.list(CHANNEL).isEmpty())
    }

    private class FailingLastRun : LastRunPort {
        override fun recordRun(
            reportDay: LocalDate,
            ranAt: Instant,
            status: KpiRunStatus,
        ): Unit = throw IllegalStateException("kpi_run insert failed")

        override fun lastSuccessfulRun(): Instant? = null
    }

    private companion object {
        const val CHANNEL = "local"
        const val NUMERIC = 4.0
        val CALCULATED_AT: Instant = Instant.parse("2026-10-12T03:15:00Z")
    }
}
