package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import java.time.format.DateTimeParseException

/** How one KPI is registered in, and measured for, the central Supabase platform. */
data class SupabaseKpi(
    val kpiId: String,
    val platformId: String,
    val kind: String,
    val unit: String,
    val aggregation: String,
    val description: String,
) {
    fun catalogBody(): Map<String, String> =
        mapOf(
            "id" to platformId,
            "kind" to kind,
            "unit" to unit,
            "aggregation" to aggregation,
            "description" to description,
        )
}

/**
 * The 10 KPIs of the engine in the platform's vocabulary (snake_case ids, numeric values).
 * The platform keeps at most 10 active KPIs per env.
 */
object SupabaseKpiCatalog {
    val entries: List<SupabaseKpi> =
        listOf(
            SupabaseKpi(
                KpiIds.EVENTS_CREATED_PER_DAY,
                "events_created_per_day",
                "business",
                "{event}",
                "sum",
                "Eventos creados durante el dia reportado.",
            ),
            SupabaseKpi(
                KpiIds.EXPENSES_REGISTERED_PER_DAY,
                "expenses_registered_per_day",
                "business",
                "{expense}",
                "sum",
                "Gastos registrados durante el dia reportado.",
            ),
            SupabaseKpi(
                KpiIds.EVENTS_WITH_EXPENSE_PERCENT,
                "events_with_expense_percent",
                "business",
                "%",
                "last",
                "Porcentaje historico de eventos con al menos un gasto, de 0 a 100.",
            ),
            SupabaseKpi(
                KpiIds.POST_EXPENSES_LATENCY_P95,
                "post_expenses_latency_p95",
                "technical",
                "ms",
                "last",
                "Latencia p95 de POST /api/events/{eventId}/expenses.",
            ),
            SupabaseKpi(
                KpiIds.ERROR_RATE_5XX_24H,
                "error_rate_5xx_24h",
                "technical",
                "1",
                "last",
                "Fraccion de respuestas 5xx de las ultimas 24 horas, de 0 a 1.",
            ),
            SupabaseKpi(
                KpiIds.BACKEND_HEALTH,
                "backend_health",
                "health",
                "1",
                "last",
                "Estado del backend segun /api/status: 1 si responde OK, 0 si no.",
            ),
            SupabaseKpi(
                KpiIds.LAST_SUCCESSFUL_RUN,
                "last_successful_run",
                "health",
                "s",
                "last",
                "Instante de la ultima corrida enviada a la plataforma, en segundos desde epoch.",
            ),
            SupabaseKpi(
                KpiIds.EVENTS_WITH_EXPENSE_IN_2_MIN_PERCENT,
                "eventos_con_gasto_en_2_min_pct",
                "business",
                "%",
                "avg",
                "Porcentaje de los eventos creados en el dia reportado cuyo primer gasto se cargo " +
                    "dentro de los 2 minutos de crearlos, de 0 a 100.",
            ),
            SupabaseKpi(
                KpiIds.SHARED_EVENTS_PERCENT,
                "eventos_compartidos_pct",
                "business",
                "%",
                "avg",
                "Porcentaje de los eventos creados en el dia reportado que tienen link de acceso generado, " +
                    "de 0 a 100.",
            ),
            SupabaseKpi(
                KpiIds.GET_BALANCES_LATENCY_P95,
                "latencia_p95_saldos",
                "technical",
                "ms",
                "last",
                "Latencia p95 de GET /api/events/{eventId}/balances.",
            ),
        )

    private val byKpiId: Map<String, SupabaseKpi> = entries.associateBy { it.kpiId }

    fun find(kpiId: String): SupabaseKpi? = byKpiId[kpiId]

    private const val HEALTHY_STATUS = "OK"

    /** Numeric value to send, or null when the platform cannot take it. */
    fun measurement(
        kpi: SupabaseKpi,
        value: KpiValue?,
    ): Double? =
        when (value) {
            null -> null
            is KpiValue.Numeric -> value.amount.takeIf { it.isFinite() }
            is KpiValue.Text -> textMeasurement(kpi.kpiId, value.text)
        }

    private fun textMeasurement(
        kpiId: String,
        text: String,
    ): Double? =
        when (kpiId) {
            KpiIds.BACKEND_HEALTH -> if (text == HEALTHY_STATUS) 1.0 else 0.0
            KpiIds.LAST_SUCCESSFUL_RUN -> epochSeconds(text)
            else -> null
        }

    private fun epochSeconds(text: String): Double? =
        try {
            Instant.parse(text).epochSecond.toDouble()
        } catch (_: DateTimeParseException) {
            null
        }
}
