package edu.austral.splitit.server.infrastructure.api.auth

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TrimmedSize.Validator::class])
annotation class TrimmedSize(
    val min: Int,
    val max: Int,
    val message: String = "trimmed length must be between {min} and {max}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
) {
    class Validator : ConstraintValidator<TrimmedSize, String> {
        private var min: Int = 0
        private var max: Int = 0

        override fun initialize(annotation: TrimmedSize) {
            min = annotation.min
            max = annotation.max
        }

        override fun isValid(
            value: String?,
            context: ConstraintValidatorContext,
        ): Boolean {
            if (value == null) {
                return true
            }
            val trimmed = value.trim()
            return value == trimmed && trimmed.length in min..max
        }
    }
}
