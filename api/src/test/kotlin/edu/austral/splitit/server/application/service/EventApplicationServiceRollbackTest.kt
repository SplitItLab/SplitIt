package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.UserService
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(EventApplicationService::class, EventService::class, EventMemberService::class, UserService::class)
@TestPropertySource(
    properties = [
        "POSTGRES_HOST=localhost",
        "POSTGRES_USER=test",
        "POSTGRES_PASSWORD=test",
        "JWT_SECRET=test-jwt-secret-that-is-long-enough",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
    ],
)
class EventApplicationServiceRollbackTest(
    @Autowired private val eventApplicationService: EventApplicationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
) {
    @MockitoSpyBean
    private lateinit var eventMemberService: EventMemberService

    private lateinit var owner: User

    @BeforeEach
    fun seedOwner() {
        eventMemberRepository.deleteAll()
        eventRepository.deleteAll()
        userRepository.deleteAll()
        owner =
            userRepository.saveAndFlush(
                User(name = "Mateo", email = "mateo-rollback@example.com", passwordHash = "hash"),
            )
    }

    @Test
    fun `createEvent rolls back the event when adding participants fails`() {
        doThrow(RuntimeException("write failed"))
            .whenever(eventMemberService)
            .addMembers(any(), any())

        assertFailsWith<RuntimeException> {
            eventApplicationService.createEvent(
                CreateEventCommand(
                    userId = requireNotNull(owner.id),
                    name = "Viaje a Mendoza",
                    baseCurrency = "ARS",
                    participantNames = listOf("Ana", "Juan"),
                ),
            )
        }

        assertEquals(0, eventRepository.count())
        assertEquals(0, eventMemberRepository.count())
        assertEquals(1, userRepository.count())
    }
}
