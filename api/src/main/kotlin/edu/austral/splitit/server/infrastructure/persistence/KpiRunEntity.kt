package edu.austral.splitit.server.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "kpi_run")
class KpiRunEntity(
    @Column(name = "report_day", nullable = false)
    var reportDay: LocalDate,
    @Column(name = "ran_at", nullable = false)
    var ranAt: Instant,
    @Column(nullable = false, length = STATUS_LENGTH)
    var status: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    companion object {
        const val STATUS_LENGTH = 16
    }
}
