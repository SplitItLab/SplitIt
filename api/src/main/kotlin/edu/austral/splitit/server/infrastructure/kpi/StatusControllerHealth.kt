package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.application.port.BackendHealthPort
import edu.austral.splitit.server.infrastructure.api.StatusController
import org.springframework.stereotype.Component

@Component
class StatusControllerHealth(
    private val statusController: StatusController,
) : BackendHealthPort {
    override fun status(): String = statusController.status()
}
