package edu.austral.splitit.server.application.kpiscan

import edu.austral.splitit.server.application.kpi.EventsCreatedPerDayKpi
import edu.austral.splitit.server.application.port.BackendHealthPort
import edu.austral.splitit.server.application.port.ErrorRatePort
import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.application.port.EventExpenseCoverage
import edu.austral.splitit.server.application.port.ExpenseRegistrationCount
import edu.austral.splitit.server.application.port.LastRunPort
import edu.austral.splitit.server.application.port.LatencyMetricsPort
import edu.austral.splitit.server.infrastructure.kpi.EmptyLatencyMetrics
import edu.austral.splitit.server.infrastructure.kpi.InMemoryErrorRate
import edu.austral.splitit.server.infrastructure.kpi.InMemoryLastRun
import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [EventsCreatedPerDayKpi::class])
class KpiScanConfig {
    @Bean
    fun eventCreationCount(): EventCreationCount = mock()

    @Bean
    fun expenseRegistrationCount(): ExpenseRegistrationCount = mock()

    @Bean
    fun eventExpenseCoverage(): EventExpenseCoverage = mock()

    @Bean
    fun latencyMetricsPort(): LatencyMetricsPort = EmptyLatencyMetrics()

    @Bean
    fun errorRatePort(): ErrorRatePort = InMemoryErrorRate()

    @Bean
    fun backendHealthPort(): BackendHealthPort = BackendHealthPort { "OK" }

    @Bean
    fun lastRunPort(): LastRunPort = InMemoryLastRun()
}
