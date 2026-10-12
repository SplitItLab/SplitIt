package edu.austral.splitit.server.infrastructure.api.kpi

import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import java.time.LocalDate

data class KpiRunResponse(
    val id: Long,
    val destination: String,
    val delivered: Boolean,
    val reportDay: LocalDate,
    val windowFrom: Instant,
    val windowTo: Instant,
    val calculatedAt: Instant,
    val results: List<KpiResultResponse>,
) {
    companion object {
        fun of(receipt: KpiRunReceipt): KpiRunResponse =
            KpiRunResponse(
                id = receipt.id,
                destination = receipt.destination,
                delivered = receipt.delivered,
                reportDay = receipt.reportDay,
                windowFrom = receipt.windowFrom,
                windowTo = receipt.windowTo,
                calculatedAt = receipt.calculatedAt,
                results =
                    receipt.results.map { result ->
                        val numeric = result.value as? KpiValue.Numeric
                        val text = result.value as? KpiValue.Text
                        KpiResultResponse(
                            id = result.id,
                            numericValue = numeric?.amount,
                            textValue = text?.text,
                        )
                    },
            )
    }
}

data class KpiResultResponse(
    val id: String,
    val numericValue: Double?,
    val textValue: String?,
)
