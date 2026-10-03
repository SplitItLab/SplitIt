package edu.austral.splitit.server.infrastructure.exchangerate

import com.fasterxml.jackson.annotation.JsonProperty
import edu.austral.splitit.server.application.exception.ExchangeRateUnavailableException
import edu.austral.splitit.server.application.port.ExchangeRateProvider
import edu.austral.splitit.server.domain.model.event.Currency
import edu.austral.splitit.server.domain.model.event.UsdExchangeRates
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Component
class OpenErApiExchangeRateProvider(
    private val exchangeRateRestClient: RestClient,
    @Value("\${exchange-rate.url}") private val url: String,
    @Value("\${exchange-rate.cache-ttl}") private val cacheTtl: Duration,
    private val clock: Clock,
) : ExchangeRateProvider {
    private var cachedRates: CachedRates? = null

    override fun rate(
        from: Currency,
        to: Currency,
    ): BigDecimal = currentRates().rateBetween(from, to) ?: throw ExchangeRateUnavailableException()

    @Synchronized
    private fun currentRates(): UsdExchangeRates {
        val now = clock.instant()
        val cached = cachedRates
        if (cached != null && now.isBefore(cached.fetchedAt.plus(cacheTtl))) {
            return cached.rates
        }

        val rates = fetchRates()
        cachedRates = CachedRates(rates = rates, fetchedAt = now)
        return rates
    }

    private fun fetchRates(): UsdExchangeRates {
        val response =
            try {
                exchangeRateRestClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .body<OpenErApiResponse>()
            } catch (exception: RestClientException) {
                throw ExchangeRateUnavailableException(exception)
            }

        if (response == null || response.result != SUCCESS_RESULT || response.baseCode != USD) {
            throw ExchangeRateUnavailableException()
        }

        val rates =
            response.rates
                .orEmpty()
                .filterKeys { it.matches(CURRENCY_REGEX) }
                .mapKeys { (code, _) -> Currency(code) }
        return UsdExchangeRates(rates)
    }

    private data class CachedRates(
        val rates: UsdExchangeRates,
        val fetchedAt: Instant,
    )

    internal data class OpenErApiResponse(
        val result: String?,
        @param:JsonProperty("base_code") val baseCode: String?,
        val rates: Map<String, BigDecimal>?,
    )

    private companion object {
        const val SUCCESS_RESULT = "success"
        const val USD = "USD"
        val CURRENCY_REGEX = Regex(Currency.VALID_REGEX)
    }
}
