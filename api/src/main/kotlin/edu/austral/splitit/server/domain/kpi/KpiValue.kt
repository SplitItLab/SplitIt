package edu.austral.splitit.server.domain.kpi

sealed interface KpiValue {
    data class Numeric(
        val amount: Double,
    ) : KpiValue

    data class Text(
        val text: String,
    ) : KpiValue
}
