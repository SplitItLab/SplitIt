package edu.austral.splitit.server.infrastructure.kpi

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryLastRunTest {
    @Test
    fun `starts empty and keeps only the latest instant`() {
        val lastRun = InMemoryLastRun()
        val first = Instant.parse("2026-10-10T15:00:00Z")
        val second = Instant.parse("2026-10-11T15:00:00Z")

        assertNull(lastRun.lastSuccessfulRun())

        lastRun.recordSuccessfulRun(first)
        assertEquals(first, lastRun.lastSuccessfulRun())

        lastRun.recordSuccessfulRun(second)
        assertEquals(second, lastRun.lastSuccessfulRun())
    }
}
