package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.validateNotZero
import java.math.BigDecimal
import java.math.RoundingMode

@ConsistentCopyVisibility
data class ExchangeQuote private constructor(
    val originalAmount: BigDecimal,
    val originalCurrency: Currency,
    val exchangeRate: BigDecimal,
    val baseAmount: BigDecimal,
    val baseCurrency: Currency,
) {
    companion object {
        const val RATE_SCALE = 6
        const val AMOUNT_SCALE = 4

        fun create(
            originalAmount: BigDecimal,
            originalCurrency: Currency,
            baseCurrency: Currency,
            rate: BigDecimal,
        ): ExchangeQuote {
            validateConvertible(originalAmount, originalCurrency)
            require(rate > BigDecimal.ZERO) {
                "Exchange rate must be greater than zero"
            }

            val roundedRate = rate.setScale(RATE_SCALE, RoundingMode.HALF_UP)
            return ExchangeQuote(
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                exchangeRate = roundedRate,
                baseAmount = originalAmount.multiply(roundedRate).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP),
                baseCurrency = baseCurrency,
            )
        }

        fun validateConvertible(
            originalAmount: BigDecimal,
            originalCurrency: Currency,
        ) {
            validateNotZero(originalAmount)
            require(originalCurrency.isSupported()) {
                "Currency ${originalCurrency.get()} is not supported"
            }
        }

        fun sameCurrency(
            originalAmount: BigDecimal,
            currency: Currency,
        ): ExchangeQuote = create(originalAmount, currency, currency, BigDecimal.ONE)
    }
}
