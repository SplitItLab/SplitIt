package edu.austral.splitit.server.application.port

import edu.austral.splitit.server.domain.model.event.Currency
import java.math.BigDecimal

interface ExchangeRateProvider {
    fun rate(
        from: Currency,
        to: Currency,
    ): BigDecimal
}
