package edu.austral.splitit.server.domain.model.event

import java.math.BigDecimal
import java.math.MathContext

class UsdExchangeRates(
    rates: Map<Currency, BigDecimal>,
) {
    private val rates: Map<Currency, BigDecimal> = rates.filterValues { it > BigDecimal.ZERO }

    fun rateBetween(
        from: Currency,
        to: Currency,
    ): BigDecimal? {
        val fromRate = rates[from]
        val toRate = rates[to]
        if (fromRate == null || toRate == null) return null
        return toRate.divide(fromRate, MathContext.DECIMAL128)
    }
}
