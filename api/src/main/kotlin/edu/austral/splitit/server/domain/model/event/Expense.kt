package edu.austral.splitit.server.domain.model.event

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
class Expense(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_member_id", nullable = false)
    var paidByMember: EventMember,
    @Column(nullable = false, length = NAME_MAX)
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
        const val NAME_MIN = 1
        const val NAME_MAX = 150
        const val CURRENCY_LENGTH = 3

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
            require(paidByMember.event == event || paidByMember.event.id == event.id) {
                "El integrante que paga debe pertenecer al mismo evento"
            }
            require(originalAmount > BigDecimal.ZERO) {
                "El monto original debe ser mayor que cero"
            }
            require(baseAmount > BigDecimal.ZERO) {
                "El monto base debe ser mayor que cero"
            }
            require(exchangeRate > BigDecimal.ZERO) {
                "La tasa de cambio debe ser mayor que cero"
            }

            val normalizedName = name.trim()
            require(normalizedName.length in NAME_MIN..NAME_MAX) {
                "El nombre del gasto debe tener entre $NAME_MIN y $NAME_MAX caracteres"
            }

            val now = Instant.now()
            return Expense(
                event = event,
                paidByMember = paidByMember,
                name = normalizedName,
                originalAmount = originalAmount,
                originalCurrency = originalCurrency.trim().uppercase(),
                exchangeRate = exchangeRate,
                baseAmount = baseAmount,
                expenseDate = expenseDate,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
