package edu.austral.splitit.server.domain.model.event

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExchangeQuoteTest {
    private val usd = Currency("USD")
    private val ars = Currency("ARS")

    @Test
    fun `create converts the amount into the base currency`() {
        val quote =
            ExchangeQuote.create(
                originalAmount = BigDecimal("10"),
                originalCurrency = usd,
                baseCurrency = ars,
                rate = BigDecimal("1523.9662"),
            )

        assertEquals(BigDecimal("10"), quote.originalAmount)
        assertEquals(usd, quote.originalCurrency)
        assertEquals(BigDecimal("1523.966200"), quote.exchangeRate)
        assertEquals(BigDecimal("15239.6620"), quote.baseAmount)
        assertEquals(ars, quote.baseCurrency)
    }

    @Test
    fun `create rounds the rate to 6 decimals and the base amount to 4 decimals`() {
        val quote =
            ExchangeQuote.create(
                originalAmount = BigDecimal("3.33"),
                originalCurrency = usd,
                baseCurrency = ars,
                rate = BigDecimal("1523.96621987"),
            )

        assertEquals(BigDecimal("1523.966220"), quote.exchangeRate)
        assertEquals(BigDecimal("5074.8075"), quote.baseAmount)
    }

    @Test
    fun `create computes the base amount with the rounded rate`() {
        val quote =
            ExchangeQuote.create(
                originalAmount = BigDecimal("1000"),
                originalCurrency = Currency("CLP"),
                baseCurrency = ars,
                rate = BigDecimal("1.0000005"),
            )

        assertEquals(BigDecimal("1.000001"), quote.exchangeRate)
        assertEquals(BigDecimal("1000.0010"), quote.baseAmount)
    }

    @Test
    fun `sameCurrency uses a rate of one`() {
        val quote = ExchangeQuote.sameCurrency(BigDecimal("5000"), ars)

        assertEquals(0, BigDecimal.ONE.compareTo(quote.exchangeRate))
        assertEquals(0, BigDecimal("5000").compareTo(quote.baseAmount))
        assertEquals(ars, quote.baseCurrency)
    }

    @Test
    fun `create rejects zero or negative amounts`() {
        listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { amount ->
            assertFailsWith<IllegalArgumentException> {
                ExchangeQuote.create(amount, usd, ars, BigDecimal("1500"))
            }
        }
    }

    @Test
    fun `create rejects unsupported currencies`() {
        assertFailsWith<IllegalArgumentException> {
            ExchangeQuote.create(BigDecimal("10"), Currency("GBP"), ars, BigDecimal("1800"))
        }
    }

    @Test
    fun `create rejects a non positive rate`() {
        assertFailsWith<IllegalArgumentException> {
            ExchangeQuote.create(BigDecimal("10"), usd, ars, BigDecimal.ZERO)
        }
    }

    @Test
    fun `all ticket currencies are supported`() {
        listOf("ARS", "USD", "EUR", "BRL", "UYU", "CLP").forEach { code ->
            ExchangeQuote.validateConvertible(BigDecimal.ONE, Currency(code))
        }
    }
}
