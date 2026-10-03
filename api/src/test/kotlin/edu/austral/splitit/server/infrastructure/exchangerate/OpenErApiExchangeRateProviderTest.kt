package edu.austral.splitit.server.infrastructure.exchangerate

import edu.austral.splitit.server.application.exception.ExchangeRateUnavailableException
import edu.austral.splitit.server.domain.model.event.Currency
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenErApiExchangeRateProviderTest {
    private val url = "https://rates.test/v6/latest/USD"
    private val cacheTtl = Duration.ofMinutes(30)
    private val clock = MutableClock(Instant.parse("2026-09-30T12:00:00Z"))

    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val provider =
        OpenErApiExchangeRateProvider(
            exchangeRateRestClient = restClientBuilder.build(),
            url = url,
            cacheTtl = cacheTtl,
            clock = clock,
        )

    private val usd = Currency("USD")
    private val ars = Currency("ARS")
    private val eur = Currency("EUR")

    @Test
    fun `returns the rate published against USD`() {
        expectRates(SUCCESS_BODY)

        assertEquals(0, BigDecimal("1523.9662").compareTo(provider.rate(usd, ars)))
        server.verify()
    }

    @Test
    fun `computes cross rates from the USD based table`() {
        expectRates(SUCCESS_BODY)

        // 1 EUR = 1523.9662 / 0.8 ARS
        assertEquals(0, BigDecimal("1904.95775").compareTo(provider.rate(eur, ars)))
        server.verify()
    }

    @Test
    fun `reuses the cached rates within the cache ttl`() {
        expectRates(SUCCESS_BODY, ExpectedCount.once())

        provider.rate(usd, ars)
        clock.advance(Duration.ofMinutes(29))
        provider.rate(eur, ars)

        server.verify()
    }

    @Test
    fun `fetches the rates again once the cache expires`() {
        expectRates(SUCCESS_BODY, ExpectedCount.twice())

        provider.rate(usd, ars)
        clock.advance(Duration.ofMinutes(30))
        provider.rate(usd, ars)

        server.verify()
    }

    @Test
    fun `provider server errors become ExchangeRateUnavailableException`() {
        server.expect(requestTo(url)).andExpect(method(HttpMethod.GET)).andRespond(withServerError())

        assertFailsWith<ExchangeRateUnavailableException> { provider.rate(usd, ars) }
    }

    @Test
    fun `an unsuccessful result becomes ExchangeRateUnavailableException`() {
        expectRates("""{ "result": "error", "error-type": "invalid-key" }""")

        assertFailsWith<ExchangeRateUnavailableException> { provider.rate(usd, ars) }
    }

    @Test
    fun `a table with another base becomes ExchangeRateUnavailableException`() {
        expectRates("""{ "result": "success", "base_code": "EUR", "rates": { "EUR": 1, "ARS": 1900 } }""")

        assertFailsWith<ExchangeRateUnavailableException> { provider.rate(eur, ars) }
    }

    @Test
    fun `a missing currency becomes ExchangeRateUnavailableException`() {
        expectRates(SUCCESS_BODY)

        assertFailsWith<ExchangeRateUnavailableException> { provider.rate(Currency("CLP"), ars) }
    }

    @Test
    fun `a failed fetch is not cached`() {
        server.expect(requestTo(url)).andRespond(withServerError())
        server.expect(requestTo(url)).andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON))

        assertFailsWith<ExchangeRateUnavailableException> { provider.rate(usd, ars) }
        assertEquals(0, BigDecimal("1523.9662").compareTo(provider.rate(usd, ars)))
        server.verify()
    }

    private fun expectRates(
        body: String,
        count: ExpectedCount = ExpectedCount.once(),
    ) {
        server
            .expect(count, requestTo(url))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))
    }

    private class MutableClock(
        private var now: Instant,
    ) : Clock() {
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    private companion object {
        const val SUCCESS_BODY =
            """{ "result": "success", "base_code": "USD", "rates": { "USD": 1, "ARS": 1523.9662, "EUR": 0.8, "XDR": 0.73 } }"""
    }
}
