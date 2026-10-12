package edu.austral.splitit.server.infrastructure.api.kpi

import edu.austral.splitit.server.application.service.RunKpiReport
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kpi-runs")
class KpiRunController(
    private val runKpiReport: RunKpiReport,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun run(
        @RequestBody(required = false) request: RunKpiRequest?,
    ): KpiRunResponse = KpiRunResponse.of(runKpiReport.run(request?.reportDay))

    @GetMapping
    fun list(): List<KpiRunResponse> = runKpiReport.list().map(KpiRunResponse::of)
}
