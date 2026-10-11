package edu.austral.splitit.server.infrastructure.kpi

import edu.austral.splitit.server.infrastructure.api.StatusController
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusControllerHealthTest {
    @Test
    fun `reads the existing status endpoint`() {
        val health = StatusControllerHealth(StatusController())

        assertEquals(StatusController.STATUS, health.status())
    }
}
