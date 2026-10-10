package edu.austral.splitit.server.domain.kpi

import java.time.Instant

data class KpiCalculationContext(
    val from: Instant,
    val to: Instant,
) {
    init {
        require(!to.isBefore(from)) {
            "KPI window end must not be before start"
        }
    }

    fun window(): ClosedRange<Instant> = from..to
}
