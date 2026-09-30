package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.ExchangeRateUnavailableException
import edu.austral.splitit.server.application.port.ExchangeRateProvider
import edu.austral.splitit.server.domain.model.event.Currency
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExchangeQuoteServiceTest {
    private val exchangeRateProvider: ExchangeRateProvider = mock()
    private val exchangeQuoteService = ExchangeQuoteService(exchangeRateProvider)

    @Test
    fun `quote in the base currency uses rate one without calling the provider`() {
        val quote = exchangeQuoteService.quote(BigDecimal("5000"), "ars", "ARS")

        assertEquals(0, BigDecimal.ONE.compareTo(quote.exchangeRate))
        assertEquals(0, BigDecimal("5000").compareTo(quote.baseAmount))
        verify(exchangeRateProvider, never()).rate(any(), any())
    }

    @Test
    fun `quote in another currency uses the provider rate`() {
        whenever(exchangeRateProvider.rate(Currency("USD"), Currency("ARS"))).thenReturn(BigDecimal("1523.9662"))

        val quote = exchangeQuoteService.quote(BigDecimal("10"), "USD", "ARS")

        assertEquals(BigDecimal("1523.966200"), quote.exchangeRate)
        assertEquals(BigDecimal("15239.6620"), quote.baseAmount)
        assertEquals("ARS", quote.baseCurrency.get())
    }

    @Test
    fun `invalid input is rejected before calling the provider`() {
        assertFailsWith<IllegalArgumentException> { exchangeQuoteService.quote(BigDecimal.ZERO, "USD", "ARS") }
        assertFailsWith<IllegalArgumentException> { exchangeQuoteService.quote(BigDecimal("-5"), "USD", "ARS") }
        assertFailsWith<IllegalArgumentException> { exchangeQuoteService.quote(BigDecimal("10"), "GBP", "ARS") }
        assertFailsWith<IllegalArgumentException> { exchangeQuoteService.quote(BigDecimal("10"), "US", "ARS") }

        verify(exchangeRateProvider, never()).rate(any(), any())
    }

    @Test
    fun `provider failures are propagated`() {
        whenever(exchangeRateProvider.rate(any(), any())).thenThrow(ExchangeRateUnavailableException())

        assertFailsWith<ExchangeRateUnavailableException> {
            exchangeQuoteService.quote(BigDecimal("10"), "USD", "ARS")
        }
    }
}
