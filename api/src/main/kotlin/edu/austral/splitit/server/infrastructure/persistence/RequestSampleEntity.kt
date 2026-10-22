package edu.austral.splitit.server.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "request_sample")
class RequestSampleEntity(
    @Column(nullable = false, length = ENDPOINT_LENGTH)
    var endpoint: String,
    @Column(name = "status_code", nullable = false)
    var statusCode: Short,
    @Column(name = "duration_us", nullable = false)
    var durationMicros: Int,
    @Column(name = "recorded_at", nullable = false)
    var recordedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    companion object {
        const val ENDPOINT_LENGTH = 255
    }
}
