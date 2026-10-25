package edu.austral.splitit.server.infrastructure.persistence

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.application.service.KpiRunStatus
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

private fun KpiValue?.toColumns(): Pair<Double?, String?> =
    when (this) {
        null -> null to null
        is KpiValue.Numeric -> amount to null
        is KpiValue.Text -> null to text
    }

private fun KpiResult.toEntity(
    snapshot: KpiSnapshotEntity,
    sortOrder: Int,
): KpiSnapshotValueEntity {
    val (numericValue, textValue) = value.toColumns()
    return KpiSnapshotValueEntity(
        snapshot = snapshot,
        kpiId = id,
        sortOrder = sortOrder,
        numericValue = numericValue,
        textValue = textValue,
    )
}

private fun KpiSnapshotEntity.toReceipt(): KpiRunReceipt {
    val storedId = checkNotNull(id) { "Stored KPI snapshot is missing an id" }
    return KpiRunReceipt(
        id = storedId,
        destination = channel,
        // The local and report channels deliver a run by storing it.
        status = KpiRunStatus.SENT,
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
