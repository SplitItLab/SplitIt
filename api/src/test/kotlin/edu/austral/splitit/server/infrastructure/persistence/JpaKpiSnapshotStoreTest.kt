package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunStatus
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
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
class JpaKpiSnapshotStoreTest(
    @Autowired private val store: JpaKpiSnapshotStore,
) {
    @Test
    fun `two days on the local channel stay listed in id order`() {
        val first = store.append("local", run(LocalDate.parse("2026-10-10")))
        val second = store.append("local", run(LocalDate.parse("2026-10-11")))

        val listed = store.list("local")

        assertEquals(listOf(first.id, second.id), listed.map { it.id })
        assertEquals(
            listOf(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-11")),
            listed.map { it.reportDay },
        )
        assertEquals("local", listed[0].destination)
        assertEquals(KpiRunStatus.SENT, first.status)
        assertEquals(KpiRunStatus.SENT, listed[0].status)
        assertEquals(WINDOW_FROM, listed[0].windowFrom)
        assertEquals(WINDOW_TO, listed[0].windowTo)
        assertEquals(CALCULATED_AT, listed[0].calculatedAt)
        assertEquals(KpiValue.Numeric(NUMERIC), listed[0].results[0].value)
        assertEquals(KpiValue.Text("OK"), listed[0].results[1].value)
        assertNull(listed[0].results[2].value)
        assertEquals(CALCULATED_AT, listed[0].results[2].calculatedAt)
    }

    @Test
    fun `list keeps local and report rows apart`() {
        store.append("local", run(LocalDate.parse("2026-10-10")))
        store.append("report", run(LocalDate.parse("2026-10-11")))

        val local = store.list("local")
        val report = store.list("report")

        assertEquals(listOf("local"), local.map { it.destination })
        assertEquals(listOf(LocalDate.parse("2026-10-10")), local.map { it.reportDay })
        assertEquals(listOf("report"), report.map { it.destination })
        assertEquals(listOf(LocalDate.parse("2026-10-11")), report.map { it.reportDay })
    }

    private fun run(day: LocalDate): KpiRun =
        KpiRun(
            reportDay = day,
            windowFrom = WINDOW_FROM,
            windowTo = WINDOW_TO,
            results =
                listOf(
                    KpiResult(KpiIds.EVENTS_CREATED_PER_DAY, KpiValue.Numeric(NUMERIC), CALCULATED_AT),
                    KpiResult(KpiIds.BACKEND_HEALTH, KpiValue.Text("OK"), CALCULATED_AT),
                    KpiResult(KpiIds.ERROR_RATE_5XX_24H, null, CALCULATED_AT),
                ),
        )

    private companion object {
        const val NUMERIC = 1.5
        val WINDOW_FROM: Instant = Instant.parse("2026-10-10T00:00:00Z")
        val WINDOW_TO: Instant = Instant.parse("2026-10-10T23:59:59.999999999Z")
        val CALCULATED_AT: Instant = Instant.parse("2026-10-12T03:15:00Z")
    }
}
