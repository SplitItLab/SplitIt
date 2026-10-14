package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.service.BalanceCalculator
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.ExpenseService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class BalanceSummary(
    val memberId: Long,
    val memberName: String,
    val netBalance: BigDecimal,
)

@Service
class BalanceApplicationService(
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
    private val expenseService: ExpenseService,
) {
    @Transactional(readOnly = true)
    fun listBalances(
        userId: Long,
        eventId: Long,
    ): List<BalanceSummary> {
        findEventForMember(userId, eventId)

        val members = eventMemberService.findByEventId(eventId)
        val expenses = expenseService.findByEventId(eventId)

        return BalanceCalculator.calculate(members, expenses).map { balance ->
            BalanceSummary(
                memberId = balance.memberId,
                memberName = balance.memberName,
                netBalance = balance.netBalance,
            )
        }
    }

    private fun findEventForMember(
        userId: Long,
        eventId: Long,
    ): Event {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        val isOwner = event.owner.id == userId
        val isMember = isOwner || eventMemberService.isUserMemberOfEvent(eventId, userId)
        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        return event
    }
}
