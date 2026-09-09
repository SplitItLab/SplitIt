package edu.austral.splitit.server.domain.model

import java.math.BigDecimal

private fun capitalize(entity: String): String =
    entity
        .replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase()
            } else {
                it.toString()
            }
        }

fun validateNotZero(x: BigDecimal) {
    require(x > BigDecimal.ZERO) {
        "Base amount must be greater than zero"
    }
}

fun validateBetween(
    entity: String,
    value: String,
    min: Int,
    max: Int,
) {
    require(value.length in min..max) {
        "${capitalize(entity)} must be between $min and $max characters"
    }
}
