package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore

class ReportKpiExporter(
    private val store: JpaKpiSnapshotStore,
) : KpiExporter {
    override val destination: String = CHANNEL

    override val delivers: Boolean = true

    override fun export(run: KpiRun): KpiRunReceipt = store.append(CHANNEL, run).copy(delivered = true)

    override fun list(): List<KpiRunReceipt> = store.list(CHANNEL).map { it.copy(delivered = true) }

    companion object {
        const val CHANNEL = "report"
    }
}
