package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.service.BalanceSummary
import edu.austral.splitit.server.application.service.EventApplicationService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventBalanceController(
    private val eventApplicationService: EventApplicationService,
) {
    @GetMapping("/{eventId}/balances")
    fun listBalances(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable eventId: Long,
    ): List<BalanceSummary> = eventApplicationService.listBalances(user.id, eventId)
}
