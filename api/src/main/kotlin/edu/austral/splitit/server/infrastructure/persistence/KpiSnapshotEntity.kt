package edu.austral.splitit.server.infrastructure.persistence

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "kpi_snapshot")
class KpiSnapshotEntity(
    @Column(nullable = false, length = CHANNEL_LENGTH)
    var channel: String,
    @Column(name = "report_day", nullable = false)
    var reportDay: LocalDate,
    @Column(name = "window_from", nullable = false)
    var windowFrom: Instant,
    @Column(name = "window_to", nullable = false)
    var windowTo: Instant,
    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "snapshot", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val values: MutableList<KpiSnapshotValueEntity> = mutableListOf()

    fun addValue(value: KpiSnapshotValueEntity) {
        value.snapshot = this
        values.add(value)
    }

    companion object {
        const val CHANNEL_LENGTH = 16
    }
}

class KpiSnapshotValueId(
    var snapshot: Long = 0,
    var kpiId: String = "",
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is KpiSnapshotValueId) {
            return false
        }
        return snapshot == other.snapshot && kpiId == other.kpiId
    }

    override fun hashCode(): Int = 31 * snapshot.hashCode() + kpiId.hashCode()

    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

@Entity
@Table(name = "kpi_snapshot_value")
@IdClass(KpiSnapshotValueId::class)
class KpiSnapshotValueEntity(
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    var snapshot: KpiSnapshotEntity,
    @Id
    @Column(name = "kpi_id", nullable = false, length = KPI_ID_LENGTH)
    var kpiId: String,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
    @Column(name = "numeric_value")
    var numericValue: Double? = null,
    @Column(name = "text_value", columnDefinition = "TEXT")
    var textValue: String? = null,
) {
    companion object {
        const val KPI_ID_LENGTH = 64
    }
}
