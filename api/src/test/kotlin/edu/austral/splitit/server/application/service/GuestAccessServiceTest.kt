package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.InviteLinkNotFoundException
import edu.austral.splitit.server.application.exception.MemberHasAccountException
import edu.austral.splitit.server.application.exception.MemberNotInEventException
import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.event.InviteLink
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.InviteLinkService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GuestAccessServiceTest {
    private val inviteLinkService: InviteLinkService = mock()
    private val eventMemberService: EventMemberService = mock()

    private val guestAccessService =
        GuestAccessService(
            inviteLinkService = inviteLinkService,
            eventMemberService = eventMemberService,
        )

    private val owner = Helpers.user()
    private val event =
        Event
            .create(owner = owner, name = "Viaje a Bariloche", baseCurrency = "ARS")
            .apply { id = 10L }
    private val inviteLink = InviteLink.create(event, TOKEN)

    @Test
    fun `enter returns the guest for a member without an account`() {
        val juan = EventMember.create(event = event, displayName = "Juan").apply { id = 30L }
        whenever(inviteLinkService.findByToken(TOKEN)).thenReturn(inviteLink)
        whenever(eventMemberService.findById(30L)).thenReturn(juan)

        val guest = guestAccessService.enter(TOKEN, 30L)

        assertEquals(AuthGuest(eventId = 10L, memberId = 30L, name = "Juan"), guest)
    }

    @Test
    fun `enter rejects an unknown invite link`() {
        whenever(inviteLinkService.findByToken(TOKEN)).thenReturn(null)

        assertFailsWith<InviteLinkNotFoundException> {
            guestAccessService.enter(TOKEN, 30L)
        }
    }

    @Test
    fun `enter rejects a member that does not exist`() {
        whenever(inviteLinkService.findByToken(TOKEN)).thenReturn(inviteLink)
        whenever(eventMemberService.findById(30L)).thenReturn(null)

        assertFailsWith<MemberNotInEventException> {
            guestAccessService.enter(TOKEN, 30L)
        }
    }

    @Test
    fun `enter rejects a member of another event`() {
        val otherEvent =
            Event
                .create(owner = owner, name = "Asado", baseCurrency = "ARS")
                .apply { id = 20L }
        val stranger = EventMember.create(event = otherEvent, displayName = "Pedro").apply { id = 40L }
        whenever(inviteLinkService.findByToken(TOKEN)).thenReturn(inviteLink)
        whenever(eventMemberService.findById(40L)).thenReturn(stranger)

        assertFailsWith<MemberNotInEventException> {
            guestAccessService.enter(TOKEN, 40L)
        }
    }

    @Test
    fun `enter rejects a member linked to an account`() {
        val ownerMember =
            EventMember.create(event = event, displayName = owner.name, user = owner).apply { id = 31L }
        whenever(inviteLinkService.findByToken(TOKEN)).thenReturn(inviteLink)
        whenever(eventMemberService.findById(31L)).thenReturn(ownerMember)

        assertFailsWith<MemberHasAccountException> {
            guestAccessService.enter(TOKEN, 31L)
        }
    }

    private companion object {
        const val TOKEN = "un-token-de-invitacion-valido"
    }
}
