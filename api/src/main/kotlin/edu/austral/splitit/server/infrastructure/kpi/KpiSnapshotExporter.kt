package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.KpiExporter
import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.infrastructure.persistence.JpaKpiSnapshotStore

class KpiSnapshotExporter(
    private val store: JpaKpiSnapshotStore,
    private val channel: String,
) : KpiExporter {
    override fun export(run: KpiRun): KpiRunReceipt = store.append(channel, run)

    override fun list(): List<KpiRunReceipt> = store.list(channel)
}
