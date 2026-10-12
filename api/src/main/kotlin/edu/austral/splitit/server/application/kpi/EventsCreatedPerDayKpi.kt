package edu.austral.splitit.server.application.kpi

import edu.austral.splitit.server.application.port.EventCreationCount
import edu.austral.splitit.server.domain.kpi.Kpi
import edu.austral.splitit.server.domain.kpi.KpiCalculationContext
import edu.austral.splitit.server.domain.kpi.KpiIds
import edu.austral.splitit.server.domain.kpi.KpiValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** Events whose createdAt falls inside the context window, inclusive. Zero events is 0.0. */
@Component
@Order(1)
class EventsCreatedPerDayKpi(
    private val eventCreationCount: EventCreationCount,
) : Kpi {
    override val id: String = KpiIds.EVENTS_CREATED_PER_DAY

    override fun calculate(context: KpiCalculationContext): KpiValue =
        KpiValue.Numeric(
            eventCreationCount.countEventsCreated(context.from, context.to).toDouble(),
        )
}
