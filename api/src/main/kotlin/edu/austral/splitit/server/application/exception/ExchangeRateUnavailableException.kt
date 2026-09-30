package edu.austral.splitit.server.application.exception

class ExchangeRateUnavailableException(
    cause: Throwable? = null,
) : RuntimeException("Exchange rate unavailable", cause)
