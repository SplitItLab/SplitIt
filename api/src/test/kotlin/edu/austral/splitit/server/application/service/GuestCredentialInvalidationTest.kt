package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.InviteLink
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import edu.austral.splitit.server.infrastructure.persistence.InviteLinkRepository
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// SPT-98: a guest credential stops working once someone links an account to its member.
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(
    GuestAccessService::class,
    InvitationApplicationService::class,
    EventMemberService::class,
    InviteLinkService::class,
    UserService::class,
)
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
class GuestCredentialInvalidationTest(
    @Autowired private val guestAccessService: GuestAccessService,
    @Autowired private val invitationApplicationService: InvitationApplicationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
    @Autowired private val inviteLinkRepository: InviteLinkRepository,
) {
    private lateinit var event: Event
    private lateinit var juan: EventMember

    @BeforeEach
    fun seedEvent() {
        inviteLinkRepository.deleteAll()
        eventMemberRepository.deleteAll()
        eventRepository.deleteAll()
        userRepository.deleteAll()
        val owner =
            userRepository.saveAndFlush(Helpers.user(name = "Mateo", email = "mateo-guest@example.com", id = null))
        event =
            eventRepository.saveAndFlush(Event.create(owner = owner, name = "Viaje a Bariloche", baseCurrency = "ARS"))
        eventMemberRepository.saveAndFlush(EventMember.create(event = event, displayName = owner.name, user = owner))
        juan = eventMemberRepository.saveAndFlush(EventMember.create(event = event, displayName = "Juan"))
        inviteLinkRepository.saveAndFlush(InviteLink.create(event, TOKEN))
    }

    @Test
    fun `the guest credential stops working once an account is linked to its member`() {
        val guest = guestAccessService.enter(TOKEN, requireNotNull(juan.id))
        assertTrue(guestAccessService.isActive(guest))

        val juanAccount =
            userRepository.saveAndFlush(Helpers.user(name = "Juan", email = "juan-guest@example.com", id = null))
        invitationApplicationService.linkMember(
            LinkMemberCommand(
                userId = requireNotNull(juanAccount.id),
                token = TOKEN,
                memberId = requireNotNull(juan.id),
            ),
        )

        assertFalse(guestAccessService.isActive(guest))
    }

    @Test
    fun `a guest credential is not active for a member of another event`() {
        val guest = guestAccessService.enter(TOKEN, requireNotNull(juan.id))

        assertFalse(guestAccessService.isActive(guest.copy(eventId = requireNotNull(event.id) + 1)))
    }

    private companion object {
        const val TOKEN = "guest-invalidation-token"
    }
}
