package edu.austral.splitit.server.application.port

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt

interface KpiExporter {
    fun export(run: KpiRun): KpiRunReceipt

    fun list(): List<KpiRunReceipt>
}
