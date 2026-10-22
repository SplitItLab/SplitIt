package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.AccountAlreadyLinkedException
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.MemberAlreadyLinkedException
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InvitationApplicationServiceTest {
    private val userService: UserService = mock()
    private val eventMemberService: EventMemberService = mock()
    private val inviteLinkService: InviteLinkService = mock()

    private val invitationApplicationService =
        InvitationApplicationService(
            userService = userService,
            eventMemberService = eventMemberService,
            inviteLinkService = inviteLinkService,
        )

    private val owner = Helpers.user(name = "Nicolas", email = "nicolas@example.com", passwordHash = "hash")
    private val guest = Helpers.user(id = 2L, name = "Sofia", email = "sofia@example.com", passwordHash = "hash")
    private val event =
        Event
            .create(
                owner = owner,
                name = "Juntada Asado",
                description = "Asado del sábado",
                iconKey = "food",
                baseCurrency = "ARS",
            ).apply { id = 10L }
    private val ownerMember = EventMember.create(event, "Nicolas", owner).apply { id = 1L }
    private val freeMember = EventMember.create(event, "Francisco").apply { id = 2L }

    @Test
    fun `getInvitation returns the event basic data and member count`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.countByEventId(10L)).thenReturn(2L)

        val invitation = invitationApplicationService.getInvitation(TOKEN)

        assertEquals(
            InvitationSummary(
                name = "Juntada Asado",
                description = "Asado del sábado",
                iconKey = "food",
                memberCount = 2L,
            ),
            invitation,
        )
    }

    @Test
    fun `getInvitation throws EventNotFoundException for an unknown token`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            invitationApplicationService.getInvitation(TOKEN)
        }
    }

    @Test
    fun `listMembers marks which members are already linked to an account`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.findByEventId(10L)).thenReturn(listOf(ownerMember, freeMember))

        val members = invitationApplicationService.listMembers(TOKEN)

        assertEquals(
            listOf(
                InvitationMemberSummary(id = 1L, name = "Nicolas", isLinked = true),
                InvitationMemberSummary(id = 2L, name = "Francisco", isLinked = false),
            ),
            members,
        )
    }

    @Test
    fun `getAccess returns the member already linked to the account`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.findByEventIdAndUserId(10L, 1L)).thenReturn(ownerMember)

        val access = invitationApplicationService.getAccess(userId = 1L, token = TOKEN)

        assertEquals(InvitationAccess(eventId = 10L, linkedMember = LinkedMemberSummary(1L, "Nicolas")), access)
    }

    @Test
    fun `getAccess returns no member when the account is not linked yet`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.findByEventIdAndUserId(10L, 2L)).thenReturn(null)

        val access = invitationApplicationService.getAccess(userId = 2L, token = TOKEN)

        assertEquals(10L, access.eventId)
        assertNull(access.linkedMember)
    }

    @Test
    fun `linkMember links the account to the chosen member`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(false)
        whenever(eventMemberService.findById(2L)).thenReturn(freeMember)
        whenever(userService.getById(2L)).thenReturn(guest)
        whenever(eventMemberService.saveAndFlush(freeMember)).thenReturn(freeMember)

        val command = LinkMemberCommand(userId = 2L, token = TOKEN, memberId = 2L)

        val access = invitationApplicationService.linkMember(command)

        assertEquals(guest, freeMember.user)
        assertEquals(InvitationAccess(eventId = 10L, linkedMember = LinkedMemberSummary(2L, "Francisco")), access)
        verify(eventMemberService).saveAndFlush(freeMember)
    }

    @Test
    fun `linkMember rejects a member already linked to another account`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(false)
        whenever(eventMemberService.findById(1L)).thenReturn(ownerMember)
        whenever(userService.getById(2L)).thenReturn(guest)

        assertFailsWith<MemberAlreadyLinkedException> {
            invitationApplicationService.linkMember(LinkMemberCommand(userId = 2L, token = TOKEN, memberId = 1L))
        }

        assertEquals(owner, ownerMember.user)
        verify(eventMemberService, never()).saveAndFlush(any())
    }

    @Test
    fun `linkMember rejects an account already linked to the event`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 1L)).thenReturn(true)

        assertFailsWith<AccountAlreadyLinkedException> {
            invitationApplicationService.linkMember(LinkMemberCommand(userId = 1L, token = TOKEN, memberId = 2L))
        }

        verify(eventMemberService, never()).saveAndFlush(any())
    }

    @Test
    fun `linkMember translates the unique constraint violation into a link conflict`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(false)
        whenever(eventMemberService.findById(2L)).thenReturn(freeMember)
        whenever(userService.getById(2L)).thenReturn(guest)
        val violation = DataIntegrityViolationException("uk_event_members_event_user")
        whenever(eventMemberService.saveAndFlush(freeMember)).thenThrow(violation)

        assertFailsWith<AccountAlreadyLinkedException> {
            invitationApplicationService.linkMember(LinkMemberCommand(userId = 2L, token = TOKEN, memberId = 2L))
        }
    }

    @Test
    fun `linkMember rejects a member from another event`() {
        val otherEvent = Event.create(owner = owner, name = "Otro", baseCurrency = "ARS").apply { id = 20L }
        val foreignMember = EventMember.create(otherEvent, "Lucas").apply { id = 5L }
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(event)
        whenever(eventMemberService.isUserMemberOfEvent(10L, 2L)).thenReturn(false)
        whenever(eventMemberService.findById(5L)).thenReturn(foreignMember)

        assertFailsWith<IllegalArgumentException> {
            invitationApplicationService.linkMember(LinkMemberCommand(userId = 2L, token = TOKEN, memberId = 5L))
        }

        verify(eventMemberService, never()).saveAndFlush(any())
    }

    @Test
    fun `linkMember throws EventNotFoundException for an unknown token`() {
        whenever(inviteLinkService.findEventByToken(TOKEN)).thenReturn(null)

        assertFailsWith<EventNotFoundException> {
            invitationApplicationService.linkMember(LinkMemberCommand(userId = 2L, token = TOKEN, memberId = 2L))
        }
    }

    private companion object {
        const val TOKEN = "r7Qk2Vx9mLpZ4tHnCwB3sdAbCdEf"
    }
}
