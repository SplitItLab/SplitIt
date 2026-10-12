package edu.austral.splitit.server.infrastructure.api.kpi

import edu.austral.splitit.server.application.service.RunKpiReport
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kpi-runs")
class KpiRunController(
    private val runKpiReport: RunKpiReport,
) {
    @GetMapping
    fun list(): List<KpiRunResponse> = runKpiReport.list().map(KpiRunResponse::of)
}
