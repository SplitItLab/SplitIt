package edu.austral.splitit.server.domain.model.event

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsdExchangeRatesTest {
    private val rates =
        UsdExchangeRates(
            mapOf(
                Currency("USD") to BigDecimal.ONE,
                Currency("ARS") to BigDecimal("1500"),
                Currency("EUR") to BigDecimal("0.8"),
            ),
        )

    @Test
    fun `rate from USD is the published rate`() {
        assertEquals(0, BigDecimal("1500").compareTo(rates.rateBetween(Currency("USD"), Currency("ARS"))))
    }

    @Test
    fun `cross rate is derived from the USD based rates`() {
        assertEquals(0, BigDecimal("1875").compareTo(rates.rateBetween(Currency("EUR"), Currency("ARS"))))
        assertEquals(0, BigDecimal("1.25").compareTo(rates.rateBetween(Currency("EUR"), Currency("USD"))))
    }

    @Test
    fun `rate is null when a currency is missing`() {
        assertNull(rates.rateBetween(Currency("BRL"), Currency("ARS")))
        assertNull(rates.rateBetween(Currency("ARS"), Currency("BRL")))
    }

    @Test
    fun `non positive published rates are ignored`() {
        val broken = UsdExchangeRates(mapOf(Currency("USD") to BigDecimal.ONE, Currency("ARS") to BigDecimal.ZERO))

        assertNull(broken.rateBetween(Currency("ARS"), Currency("USD")))
    }
}
