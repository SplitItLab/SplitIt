package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.InviteLink
import edu.austral.splitit.server.infrastructure.persistence.InviteLinkRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InviteLinkServiceTest {
    private val inviteLinkRepository: InviteLinkRepository = mock()
    private val inviteLinkService = InviteLinkService(inviteLinkRepository)

    private val owner = Helpers.user(name = "Dueño", email = "dueno@example.com", passwordHash = "hash")

    private fun event(id: Long): Event =
        Event
            .create(owner = owner, name = "Viaje", baseCurrency = "ARS")
            .apply { this.id = id }

    private fun echoSavedLink() {
        whenever(inviteLinkRepository.save(any<InviteLink>())).thenAnswer { it.getArgument<InviteLink>(0) }
    }

    @Test
    fun `getOrCreate returns the existing invite link without creating a new one`() {
        val event = event(10L)
        val existing = InviteLink.create(event, "un-token-ya-persistido-123")
        whenever(inviteLinkRepository.findByEventId(10L)).thenReturn(existing)

        val link = inviteLinkService.getOrCreate(event)

        assertSame(existing, link)
        verify(inviteLinkRepository, never()).save(any<InviteLink>())
    }

    @Test
    fun `getOrCreate creates an invite link when the event has none`() {
        val event = event(10L)
        whenever(inviteLinkRepository.findByEventId(10L)).thenReturn(null)
        echoSavedLink()

        val link = inviteLinkService.getOrCreate(event)

        assertSame(event, link.event)
        assertEquals(43, link.token.length)
        verify(inviteLinkRepository).save(any<InviteLink>())
    }

    @Test
    fun `generated tokens are unique and not derived from the event id`() {
        whenever(inviteLinkRepository.findByEventId(any())).thenReturn(null)
        echoSavedLink()

        val first = inviteLinkService.getOrCreate(event(10L)).token
        val second = inviteLinkService.getOrCreate(event(10L)).token
        val other = inviteLinkService.getOrCreate(event(11L)).token

        assertNotEquals(first, second)
        assertNotEquals(first, other)
        listOf(first, second, other).forEach { token ->
            assertTrue(token.length >= InviteLink.TOKEN_MIN_LENGTH, "Token too short: $token")
            assertTrue(token.none { it == '+' || it == '/' || it == '=' }, "Token is not base64url: $token")
        }
    }
}
