package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.event.Currency.Companion.CURRENCY_LENGTH
import edu.austral.splitit.server.domain.model.validateBetween
import edu.austral.splitit.server.domain.model.validateNotZero
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "expenses",
    indexes = [
        Index(name = "idx_expenses_event_id", columnList = "event_id"),
        Index(name = "idx_expenses_paid_by_member_id", columnList = "paid_by_member_id"),
    ],
)
class Expense private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_member_id", nullable = false)
    var paidByMember: EventMember,
    @Column(nullable = false, length = EXPENSE_NAME_MAX)
    var name: String,
    @Column(name = "original_amount", nullable = false, precision = 19, scale = 4)
    var originalAmount: BigDecimal,
    @Column(name = "original_currency", nullable = false, length = CURRENCY_LENGTH)
    var originalCurrency: String,
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 6)
    var exchangeRate: BigDecimal = BigDecimal.ONE,
    @Column(name = "base_amount", nullable = false, precision = 19, scale = 4)
    var baseAmount: BigDecimal,
    @Column(name = "expense_date", nullable = false)
    var expenseDate: LocalDate,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    companion object {
        const val EXPENSE_NAME_MIN = 1
        const val EXPENSE_NAME_MAX = 150

        const val AMOUNT_PRECISION = 19
        const val AMOUNT_SCALE = 4

        const val RATE_PRECISION = 19
        const val RATE_SCALE = 6

        fun create(
            event: Event,
            paidByMember: EventMember,
            name: String,
            originalAmount: BigDecimal,
            originalCurrency: String,
            exchangeRate: BigDecimal = BigDecimal.ONE,
            baseAmount: BigDecimal,
            expenseDate: LocalDate,
        ): Expense {
            require(
                paidByMember.event === event ||
                    (paidByMember.event.id != null && paidByMember.event.id == event.id),
            ) {
                "Paying member must belong to the same event"
            }

            validateNotZero(originalAmount)
            validateNotZero(baseAmount)
            validateNotZero(exchangeRate)

            requireFitsNumeric(originalAmount, AMOUNT_PRECISION, AMOUNT_SCALE, "Original amount")
            requireFitsNumeric(baseAmount, AMOUNT_PRECISION, AMOUNT_SCALE, "Base amount")
            requireFitsNumeric(exchangeRate, RATE_PRECISION, RATE_SCALE, "Exchange rate")

            val normalizedName = name.trim()
            validateBetween(
                "expense name",
                normalizedName,
                EXPENSE_NAME_MIN,
                EXPENSE_NAME_MAX,
            )

            val currency = Currency(originalCurrency)

            val now = Instant.now()
            return Expense(
                event = event,
                paidByMember = paidByMember,
                name = normalizedName,
                originalAmount = originalAmount,
                originalCurrency = currency.get(),
                exchangeRate = exchangeRate,
                baseAmount = baseAmount,
                expenseDate = expenseDate,
                createdAt = now,
                updatedAt = now,
            )
        }

        private fun requireFitsNumeric(
            value: BigDecimal,
            precision: Int,
            scale: Int,
            label: String,
        ) {
            require(value.scale() <= scale) {
                "$label cannot have more than $scale decimal places"
            }
            val integerDigits = value.precision() - value.scale()
            require(integerDigits <= precision - scale) {
                "$label exceeds precision of $precision digits"
            }
        }
    }
}
