package edu.austral.splitit.server.domain.model.event

@ConsistentCopyVisibility
data class Currency private constructor(
    val currency: String,
) {
    fun get(): String = currency

    companion object {
        const val CURRENCY_LENGTH = 3
        const val VALID_REGEX = "^[A-Z]{$CURRENCY_LENGTH}$"

        operator fun invoke(rawCurrency: String): Currency {
            val normalizedCurrency = rawCurrency.trim().uppercase()

            require(
                normalizedCurrency.matches(
                    Regex(VALID_REGEX),
                ),
            ) {
                "Base currency must be a $CURRENCY_LENGTH-character ISO 4217 code"
            }

            return Currency(normalizedCurrency)
        }
    }
}
