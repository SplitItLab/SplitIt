package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.kpiscan.KpiScanConfig
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiIds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [KpiScanConfig::class])
class KpiComponentScanTest(
    @Autowired private val kpis: List<Kpi>,
) {
    @Test
    fun `production scan exposes the seven kpis in order`() {
        assertEquals(
            listOf(
                KpiIds.EVENTS_CREATED_PER_DAY,
                KpiIds.EXPENSES_REGISTERED_PER_DAY,
                KpiIds.EVENTS_WITH_EXPENSE_PERCENT,
                KpiIds.POST_EXPENSES_LATENCY_P95,
                KpiIds.ERROR_RATE_5XX_24H,
                KpiIds.BACKEND_HEALTH,
                KpiIds.LAST_SUCCESSFUL_RUN,
            ),
            kpis.map { it.id },
        )
    }
}
