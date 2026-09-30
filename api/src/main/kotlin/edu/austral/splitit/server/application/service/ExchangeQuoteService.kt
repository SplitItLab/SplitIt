package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.port.ExchangeRateProvider
import edu.austral.splitit.server.domain.model.event.Currency
import edu.austral.splitit.server.domain.model.event.ExchangeQuote
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ExchangeQuoteService(
    private val exchangeRateProvider: ExchangeRateProvider,
) {
    fun quote(
        amount: BigDecimal,
        currency: String,
        baseCurrency: String,
    ): ExchangeQuote {
        val originalCurrency = Currency(currency)
        val eventCurrency = Currency(baseCurrency)

        ExchangeQuote.validateConvertible(amount, originalCurrency)

        if (originalCurrency == eventCurrency) {
            return ExchangeQuote.sameCurrency(amount, originalCurrency)
        }

        return ExchangeQuote.create(
            originalAmount = amount,
            originalCurrency = originalCurrency,
            baseCurrency = eventCurrency,
            rate = exchangeRateProvider.rate(originalCurrency, eventCurrency),
        )
    }
}
