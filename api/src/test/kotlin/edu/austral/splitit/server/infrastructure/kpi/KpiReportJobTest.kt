package edu.austral.splitit.server.infrastructure.kpi

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import edu.austral.splitit.server.application.service.KpiRunReceipt
import edu.austral.splitit.server.application.service.KpiRunStatus
import edu.austral.splitit.server.application.service.RunKpiReport
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiResult
import edu.austral.splitit.server.domain.kpi.KpiValue
import edu.austral.splitit.server.infrastructure.persistence.JpaRequestMetrics
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class KpiReportJobTest {
    private val runKpiReport = mock<RunKpiReport>()
    private val requestMetrics = mock<JpaRequestMetrics>()
    private val job = KpiReportJob(runKpiReport, requestMetrics, Clock.fixed(CALCULATED_AT, ZoneOffset.UTC))

    @Test
    fun `daily run asks for yesterday and does not swallow a failure`() {
        whenever(runKpiReport.run(null)).thenReturn(receipt())

        job.runDaily()

        verify(runKpiReport).run(null)
    }

    @Test
    fun `daily run logs each kpi value to the console`() {
        whenever(runKpiReport.run(null)).thenReturn(
            receipt(
                results =
                    listOf(
                        KpiResult(KpiIds.EVENTS_CREATED_PER_DAY, KpiValue.Numeric(NUMERIC), CALCULATED_AT),
                        KpiResult(KpiIds.BACKEND_HEALTH, KpiValue.Text("OK"), CALCULATED_AT),
                        KpiResult(KpiIds.ERROR_RATE_5XX_24H, null, CALCULATED_AT),
                    ),
            ),
        )

        val messages = captureLogs { job.runDaily() }

        assertEquals(
            listOf(
                "KPI run stored. destination=report reportDay=2026-10-11 id=3 kpiCount=3",
                "KPI value. id=events-created-per-day value=4.0",
                "KPI value. id=backend-health value=OK",
                "KPI value. id=error-rate-5xx-24h value=null",
            ),
            messages,
        )
    }

    @Test
    fun `daily run deletes the request samples older than seven days after the run`() {
        whenever(runKpiReport.run(null)).thenReturn(receipt())

        job.runDaily()

        inOrder(runKpiReport, requestMetrics) {
            verify(runKpiReport).run(null)
            verify(requestMetrics).deleteRecordedBefore(Instant.parse("2026-10-05T03:15:00Z"))
        }
    }

    @Test
    fun `daily run propagates an exporter failure`() {
        whenever(runKpiReport.run(null)).thenThrow(IllegalStateException("store down"))

        assertFailsWith<IllegalStateException> {
            job.runDaily()
        }
    }

    @Test
    fun `daily run deletes old request samples even when the report fails`() {
        whenever(runKpiReport.run(null)).thenThrow(IllegalStateException("store down"))

        assertFailsWith<IllegalStateException> {
            job.runDaily()
        }

        verify(requestMetrics).deleteRecordedBefore(Instant.parse("2026-10-05T03:15:00Z"))
    }

    @Test
    fun `daily run keeps the report failure when the cleanup also fails`() {
        val reportFailure = IllegalStateException("store down")
        val cleanupFailure = IllegalArgumentException("delete failed")
        whenever(runKpiReport.run(null)).thenThrow(reportFailure)
        doThrow(cleanupFailure).whenever(requestMetrics).deleteRecordedBefore(any())

        val thrown =
            assertFailsWith<IllegalStateException> {
                job.runDaily()
            }

        assertSame(reportFailure, thrown)
        assertSame(cleanupFailure, thrown.suppressed.single())
    }

    @Test
    fun `daily run propagates a cleanup failure after a successful report`() {
        whenever(runKpiReport.run(null)).thenReturn(receipt())
        doThrow(IllegalArgumentException("delete failed")).whenever(requestMetrics).deleteRecordedBefore(any())

        assertFailsWith<IllegalArgumentException> {
            job.runDaily()
        }
    }

    private fun receipt(results: List<KpiResult> = emptyList()): KpiRunReceipt =
        KpiRunReceipt(
            id = 3,
            destination = "report",
            status = KpiRunStatus.SENT,
            reportDay = LocalDate.parse("2026-10-11"),
            windowFrom = Instant.parse("2026-10-11T00:00:00Z"),
            windowTo = Instant.parse("2026-10-11T23:59:59.999999999Z"),
            calculatedAt = CALCULATED_AT,
            results = results,
        )

    private fun captureLogs(block: () -> Unit): List<String> {
        val logger = LoggerFactory.getLogger(KpiReportJob::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        val previousLevel = logger.level
        appender.start()
        logger.addAppender(appender)
        logger.level = Level.INFO
        try {
            block()
        } finally {
            logger.detachAppender(appender)
            logger.level = previousLevel
            appender.stop()
        }
        return appender.list.map { it.formattedMessage }
    }

    private companion object {
        const val NUMERIC = 4.0
        val CALCULATED_AT: Instant = Instant.parse("2026-10-12T03:15:00Z")
    }
}
