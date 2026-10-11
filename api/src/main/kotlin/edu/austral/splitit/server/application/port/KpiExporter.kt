package edu.austral.splitit.server.application.port

import edu.austral.splitit.server.application.service.KpiRun
import edu.austral.splitit.server.application.service.KpiRunReceipt

interface KpiExporter {
    val destination: String

    val delivers: Boolean

    fun export(run: KpiRun): KpiRunReceipt

    fun list(): List<KpiRunReceipt>
}
