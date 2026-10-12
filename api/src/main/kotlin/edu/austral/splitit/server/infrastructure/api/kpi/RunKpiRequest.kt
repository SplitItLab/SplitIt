package edu.austral.splitit.server.infrastructure.api.kpi

import java.time.LocalDate

data class RunKpiRequest(
    val reportDay: LocalDate? = null,
)
