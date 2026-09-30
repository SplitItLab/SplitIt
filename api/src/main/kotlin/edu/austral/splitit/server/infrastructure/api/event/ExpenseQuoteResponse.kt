package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.domain.model.event.ExchangeQuote
import java.math.BigDecimal

data class ExpenseQuoteResponse(
    val originalAmount: BigDecimal,
    val originalCurrency: String,
    val exchangeRate: BigDecimal,
    val baseAmount: BigDecimal,
    val baseCurrency: String,
) {
    companion object {
        fun of(quote: ExchangeQuote): ExpenseQuoteResponse =
            ExpenseQuoteResponse(
                originalAmount = quote.originalAmount,
                originalCurrency = quote.originalCurrency.get(),
                exchangeRate = quote.exchangeRate,
                baseAmount = quote.baseAmount,
                baseCurrency = quote.baseCurrency.get(),
            )
    }
}
