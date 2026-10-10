package edu.austral.splitit.server.domain.kpi

interface Kpi {
    val id: String

    fun calculate(context: KpiCalculationContext): KpiValue?
}
