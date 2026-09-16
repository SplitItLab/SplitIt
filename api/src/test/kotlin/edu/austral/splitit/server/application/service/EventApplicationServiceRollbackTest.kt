package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.EventDeletionConflictException
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Expense
import edu.austral.splitit.server.domain.model.event.InviteLink
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.UserService
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import edu.austral.splitit.server.infrastructure.persistence.ExpenseRepository
import edu.austral.splitit.server.infrastructure.persistence.InviteLinkRepository
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
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    @Autowired private val expenseRepository: ExpenseRepository,
    @Autowired private val inviteLinkRepository: InviteLinkRepository,
) {
    @MockitoSpyBean
    private lateinit var eventMemberService: EventMemberService

    private lateinit var owner: User

    @BeforeEach
    fun seedOwner() {
        expenseRepository.deleteAll()
        inviteLinkRepository.deleteAll()
        eventMemberRepository.deleteAll()
        eventRepository.deleteAll()
        userRepository.deleteAll()
        owner =
            userRepository.saveAndFlush(
                Helpers.user(
                    name = "Mateo",
                    email = "mateo-rollback@example.com",
                    passwordHash = "hash",
                    id = null,
                ),
            )
    }

    @Test
    fun `deleteEvent removes the event and all members but preserves users and other events`() {
        val event = createEvent()
        val otherEvent = createEvent()

        eventApplicationService.deleteEvent(requireNotNull(owner.id), event.id)

        assertFalse(eventRepository.existsById(event.id))
        assertEquals(0, eventMemberRepository.countByEventId(event.id))
        assertTrue(eventRepository.existsById(otherEvent.id))
        assertEquals(3, eventMemberRepository.countByEventId(otherEvent.id))
        assertTrue(userRepository.existsById(requireNotNull(owner.id)))
        assertFailsWith<EventNotFoundException> {
            eventApplicationService.deleteEvent(requireNotNull(owner.id), event.id)
        }
    }

    @Test
    fun `deleteEvent rejects outsiders and linked members without deleting data`() {
        val event = createEvent()
        val otherUser = userRepository.saveAndFlush(Helpers.user(email = "other@example.com", id = null))
        val otherUserId = requireNotNull(otherUser.id)

        assertFailsWith<AccessDeniedException> {
            eventApplicationService.deleteEvent(otherUserId, event.id)
        }
        eventMemberService.addMember(eventRepository.findById(event.id).orElseThrow(), "Other", otherUser)
        assertFailsWith<AccessDeniedException> {
            eventApplicationService.deleteEvent(otherUserId, event.id)
        }

        assertTrue(eventRepository.existsById(event.id))
        assertEquals(4, eventMemberRepository.countByEventId(event.id))
    }

    @Test
    fun `deleteEvent rejects events with expenses and preserves all data`() {
        val event = createEvent()
        val persistedEvent = eventRepository.findById(event.id).orElseThrow()
        val member = eventMemberRepository.findAllByEventId(event.id).first()
        val expense =
            expenseRepository.saveAndFlush(
                Expense.create(persistedEvent, member, "Dinner", BigDecimal.TEN, "ARS", expenseDate = LocalDate.now()),
            )

        assertFailsWith<EventDeletionConflictException> {
            eventApplicationService.deleteEvent(requireNotNull(owner.id), event.id)
        }

        assertTrue(eventRepository.existsById(event.id))
        assertEquals(3, eventMemberRepository.countByEventId(event.id))
        assertTrue(expenseRepository.existsById(requireNotNull(expense.id)))
    }

    @Test
    fun `deleteEvent rolls back member deletion when an existing relation blocks event deletion`() {
        val event = createEvent()
        val invite =
            inviteLinkRepository.saveAndFlush(
                InviteLink.create(eventRepository.findById(event.id).orElseThrow(), "test-invite"),
            )

        assertFailsWith<EventDeletionConflictException> {
            eventApplicationService.deleteEvent(requireNotNull(owner.id), event.id)
        }

        assertTrue(eventRepository.existsById(event.id))
        assertEquals(3, eventMemberRepository.countByEventId(event.id))
        assertTrue(inviteLinkRepository.existsById(requireNotNull(invite.id)))
    }

    private fun createEvent(): EventSummary =
        eventApplicationService.createEvent(
            CreateEventCommand(
                userId = requireNotNull(owner.id),
                name = "Viaje a Mendoza",
                baseCurrency = "ARS",
                participantNames = listOf("Ana", "Juan"),
            ),
        )

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
