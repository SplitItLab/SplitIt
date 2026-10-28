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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(
    InvitationApplicationService::class,
    EventMemberService::class,
    InviteLinkService::class,
    UserService::class,
)
@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
    ],
)
class InvitationApplicationServiceConcurrencyTest(
    @Autowired private val service: InvitationApplicationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val memberRepository: EventMemberRepository,
    @Autowired private val inviteLinkRepository: InviteLinkRepository,
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    @MockitoSpyBean
    private lateinit var memberService: EventMemberService

    @Test
    fun `concurrent link waits for the committed member and rejects overwriting its account`() {
        val firstUser = userRepository.saveAndFlush(Helpers.user(email = "first@example.com", id = null))
        val secondUser = userRepository.saveAndFlush(Helpers.user(email = "second@example.com", id = null))
        val event = eventRepository.saveAndFlush(Event.create(firstUser, "Viaje", baseCurrency = "ARS"))
        val member = memberRepository.saveAndFlush(EventMember.create(event, "Ana"))
        val token = "concurrent-invitation-token"
        inviteLinkRepository.saveAndFlush(InviteLink.create(event, token))
        val eventId = requireNotNull(event.id)
        val memberId = requireNotNull(member.id)
        val firstUserId = requireNotNull(firstUser.id)
        val secondUserId = requireNotNull(secondUser.id)
        val secondAccountChecked = CountDownLatch(1)
        doAnswer { invocation ->
            val linked = invocation.callRealMethod()
            secondAccountChecked.countDown()
            linked
        }.whenever(memberService).isUserMemberOfEvent(eventId, secondUserId)

        val executor = Executors.newSingleThreadExecutor()
        try {
            val competingRequest =
                requireNotNull(
                    TransactionTemplate(transactionManager).execute {
                        service.linkMember(LinkMemberCommand(firstUserId, token, memberId))
                        val request =
                            executor.submit<InvitationAccess> {
                                service.linkMember(LinkMemberCommand(secondUserId, token, memberId))
                            }
                        assertTrue(secondAccountChecked.await(5, TimeUnit.SECONDS))
                        // Keep the first transaction open while the second request reaches the locked row.
                        assertFailsWith<TimeoutException> { request.get(500, TimeUnit.MILLISECONDS) }
                        request
                    },
                )

            val failure =
                assertFailsWith<ExecutionException> {
                    competingRequest.get(5, TimeUnit.SECONDS)
                }
            assertIs<MemberAlreadyLinkedException>(failure.cause)
            assertEquals(
                firstUserId,
                memberRepository
                    .findById(memberId)
                    .orElseThrow()
                    .user
                    ?.id,
            )
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            inviteLinkRepository.deleteAll()
            memberRepository.deleteAll()
            eventRepository.deleteAll()
            userRepository.deleteAll()
        }
    }
}
