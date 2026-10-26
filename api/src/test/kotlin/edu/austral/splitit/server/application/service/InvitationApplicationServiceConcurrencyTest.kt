package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.InviteLink
import edu.austral.splitit.server.domain.model.event.MemberAlreadyLinkedException
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import edu.austral.splitit.server.infrastructure.persistence.EventRepository
import edu.austral.splitit.server.infrastructure.persistence.InviteLinkRepository
import edu.austral.splitit.server.infrastructure.persistence.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(InvitationApplicationService::class, EventMemberService::class, InviteLinkService::class, UserService::class)
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InvitationApplicationServiceConcurrencyTest(
    @Autowired private val invitationApplicationService: InvitationApplicationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val eventMemberRepository: EventMemberRepository,
    @Autowired private val inviteLinkRepository: InviteLinkRepository,
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    @Test
    fun `concurrent claims preserve the first account and reject the second`() {
        val firstUser = userRepository.saveAndFlush(Helpers.user(email = "first@example.com", id = null))
        val secondUser = userRepository.saveAndFlush(Helpers.user(email = "second@example.com", id = null))
        val event = eventRepository.saveAndFlush(Event.create(firstUser, "Viaje", baseCurrency = "ARS"))
        val member = eventMemberRepository.saveAndFlush(EventMember.create(event, "Ana"))
        val token = "concurrent-invitation-token-123456"
        inviteLinkRepository.saveAndFlush(InviteLink.create(event, token))
        val firstCommand = LinkMemberCommand(requireNotNull(firstUser.id), token, requireNotNull(member.id))
        val secondCommand = firstCommand.copy(userId = requireNotNull(secondUser.id))

        Executors.newSingleThreadExecutor().use { executor ->
            val contender =
                TransactionTemplate(transactionManager).execute {
                    invitationApplicationService.linkMember(firstCommand)

                    val started = CountDownLatch(1)
                    val pending =
                        executor.submit<Throwable?> {
                            started.countDown()
                            runCatching { invitationApplicationService.linkMember(secondCommand) }.exceptionOrNull()
                        }
                    assertTrue(started.await(5, TimeUnit.SECONDS))
                    assertFailsWith<TimeoutException> { pending.get(500, TimeUnit.MILLISECONDS) }
                    pending
                }

            assertIs<MemberAlreadyLinkedException>(requireNotNull(contender).get(5, TimeUnit.SECONDS))
        }

        assertEquals(
            LinkedMemberSummary(requireNotNull(member.id), "Ana"),
            invitationApplicationService.getAccess(firstCommand.userId, token).linkedMember,
        )
        assertNull(invitationApplicationService.getAccess(secondCommand.userId, token).linkedMember)
    }
}
