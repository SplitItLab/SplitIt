package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.port.AuthPrincipal
import edu.austral.splitit.server.application.service.SettlementApplicationService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventSettlementController(
    private val settlementApplicationService: SettlementApplicationService,
) {
    @GetMapping("/{eventId}/suggested-payments")
    fun listSuggestedPayments(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable eventId: Long,
    ): List<SuggestedPaymentResponse> =
        settlementApplicationService
            .listSuggestedPayments(principal, eventId)
            .map { SuggestedPaymentResponse.of(it) }
}
