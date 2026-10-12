package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
@Transactional
class JpaKpiSnapshotStore(
    private val snapshots: KpiSnapshotJpaRepository,
) {
    fun append(
        channel: String,
        run: KpiRun,
    ): KpiRunReceipt {
        require(run.results.isNotEmpty()) {
            "A KPI run must include at least one result"
        }
        val snapshot =
            KpiSnapshotEntity(
                channel = channel,
                reportDay = run.reportDay,
                windowFrom = run.windowFrom,
                windowTo = run.windowTo,
                calculatedAt = run.results.first().calculatedAt,
            )
        run.results.forEachIndexed { index, result ->
            snapshot.addValue(result.toEntity(snapshot, index))
        }
        return snapshots.saveAndFlush(snapshot).toReceipt()
    }

    @Transactional(readOnly = true)
    fun list(channel: String): List<KpiRunReceipt> {
        val stored = snapshots.findAllByChannelOrderByIdAsc(channel)
        return stored.map { it.toReceipt() }
    }
}

private fun KpiResult.toEntity(
    snapshot: KpiSnapshotEntity,
    sortOrder: Int,
): KpiSnapshotValueEntity {
    val numeric = value as? KpiValue.Numeric
    val text = value as? KpiValue.Text
    return KpiSnapshotValueEntity(
        snapshot = snapshot,
        kpiId = id,
        sortOrder = sortOrder,
        numericValue = numeric?.amount,
        textValue = text?.text,
    )
}

private fun KpiSnapshotEntity.toReceipt(): KpiRunReceipt {
    val storedId = checkNotNull(id) { "Stored KPI snapshot is missing an id" }
    return KpiRunReceipt(
        id = storedId,
        destination = channel,
        delivered = false,
        reportDay = reportDay,
        windowFrom = windowFrom,
        windowTo = windowTo,
        calculatedAt = calculatedAt,
        results = values.sortedBy { it.sortOrder }.map { it.toResult(calculatedAt) },
    )
}

private fun KpiSnapshotValueEntity.toResult(calculatedAt: Instant): KpiResult {
    val numeric = numericValue
    val text = textValue
    val stored =
        when {
            numeric != null -> KpiValue.Numeric(numeric)
            text != null -> KpiValue.Text(text)
            else -> null
        }
    return KpiResult(id = kpiId, value = stored, calculatedAt = calculatedAt)
}
