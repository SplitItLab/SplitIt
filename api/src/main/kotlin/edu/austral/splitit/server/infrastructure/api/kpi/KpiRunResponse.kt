package edu.austral.splitit.server.infrastructure.api.kpi

import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import java.time.Instant
import java.time.LocalDate

data class KpiRunResponse(
    val id: Long,
    val destination: String,
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
                reportDay = receipt.reportDay,
                windowFrom = receipt.windowFrom,
                windowTo = receipt.windowTo,
                calculatedAt = receipt.calculatedAt,
                results = receipt.results.map { result -> result.toResponse() },
            )
    }
}

private fun KpiResult.toResponse(): KpiResultResponse {
    val (numericValue, textValue) =
        when (val stored = value) {
            null -> null to null
            is KpiValue.Numeric -> stored.amount to null
            is KpiValue.Text -> null to stored.text
        }
    return KpiResultResponse(
        id = id,
        numericValue = numericValue,
        textValue = textValue,
    )
}

data class KpiResultResponse(
    val id: String,
    val numericValue: Double?,
    val textValue: String?,
)
