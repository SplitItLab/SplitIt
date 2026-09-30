package edu.austral.splitit.server.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Duration

@Configuration
class ExchangeRateConfig(
    @Value("\${exchange-rate.timeout}") private val timeout: Duration,
) {
    @Bean
    fun exchangeRateRestClient(): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(timeout)
                setReadTimeout(timeout)
            }
        return RestClient.builder().requestFactory(requestFactory).build()
    }

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
