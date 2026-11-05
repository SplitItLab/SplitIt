package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.port.GoogleIdentity
import edu.austral.splitit.server.application.port.GoogleTokenVerifier
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.EventTransferService
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(GoogleLoginService::class, UserService::class, EventTransferService::class)
@TestPropertySource(
    properties = [
        "POSTGRES_HOST=localhost",
        "POSTGRES_USER=test",
        "POSTGRES_PASSWORD=test",
        "JWT_SECRET=test-jwt-secret-that-is-long-enough",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
    ],
)
class GoogleLoginServiceRollbackTest(
    @Autowired private val googleLoginService: GoogleLoginService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
) {
    @MockitoBean
    private lateinit var googleTokenVerifier: GoogleTokenVerifier

    @MockitoSpyBean
    private lateinit var eventTransferService: EventTransferService

    private lateinit var passwordUser: User
    private lateinit var event: Event

    @BeforeEach
    fun seedPasswordAccountWithAnEvent() {
        eventMemberRepository.deleteAll()
        eventRepository.deleteAll()
        userRepository.deleteAll()
        whenever(googleTokenVerifier.verify("google.id.token"))
            .thenReturn(GoogleIdentity("google-subject-1", "ada@example.com", "Ada Google"))

        passwordUser = userRepository.saveAndFlush(Helpers.user(id = null))
        event = eventRepository.saveAndFlush(Event.create(passwordUser, "Viaje", baseCurrency = "ARS"))
        eventMemberRepository.saveAndFlush(EventMember.create(event, "Ada", passwordUser))
    }

    @Test
    fun `replacing moves the events to a new account and disables the old one`() {
        val replacement = googleLoginService.login("google.id.token", ExistingAccountChoice.REPLACE)

        val newId = requireNotNull(replacement.id)
        val oldId = requireNotNull(passwordUser.id)
        assertEquals("Ada Google", replacement.name)
        assertEquals("ada@example.com", replacement.email)
        assertEquals(newId, userRepository.findByGoogleSubject("google-subject-1")?.id)
        assertEquals(
            newId,
            eventRepository
                .findById(requireNotNull(event.id))
                .orElseThrow()
                .owner.id,
        )
        val memberUserIds = eventMemberRepository.findAllByEventId(requireNotNull(event.id)).map { it.user?.id }
        assertEquals(listOf(newId), memberUserIds)

        val disabled = userRepository.findById(oldId).orElseThrow()
        assertNotNull(disabled.disabledAt)
        assertNull(disabled.passwordHash)
        assertNull(disabled.googleSubject)
        assertEquals("deleted-$oldId@splitit.invalid", disabled.email)
    }

    @Test
    fun `a failure while moving the events leaves no half-made account`() {
        doThrow(IllegalStateException("boom")).whenever(eventTransferService).transferAll(any(), any())

        assertFailsWith<IllegalStateException> {
            googleLoginService.login("google.id.token", ExistingAccountChoice.REPLACE)
        }

        val untouched = userRepository.findById(requireNotNull(passwordUser.id)).orElseThrow()
        assertEquals("ada@example.com", untouched.email)
        assertEquals("hashed", untouched.passwordHash)
        assertNull(untouched.disabledAt)
        assertNull(userRepository.findByGoogleSubject("google-subject-1"))
        assertEquals(1, userRepository.count())
        assertEquals(
            untouched.id,
            eventRepository
                .findById(requireNotNull(event.id))
                .orElseThrow()
                .owner.id,
        )
    }

    @Test
    fun `linking keeps the account and its events and drops the password`() {
        val linked = googleLoginService.login("google.id.token", ExistingAccountChoice.LINK)

        assertEquals(passwordUser.id, linked.id)
        assertEquals("Ada Lovelace", linked.name)
        val stored = userRepository.findById(requireNotNull(passwordUser.id)).orElseThrow()
        assertEquals("google-subject-1", stored.googleSubject)
        assertNull(stored.passwordHash)
        assertEquals(
            passwordUser.id,
            eventRepository
                .findById(requireNotNull(event.id))
                .orElseThrow()
                .owner.id,
        )
    }
}
