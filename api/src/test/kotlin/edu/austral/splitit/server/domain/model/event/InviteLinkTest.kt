package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.Helpers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class InviteLinkTest {
    private val owner = Helpers.user(name = "Dueño", email = "dueno@example.com", passwordHash = "hash")
    private val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")

    @Test
    fun `create invite link successfully`() {
        val token = "opaque-token-1234567890"

        val link = InviteLink.create(event = event, token = token)

        assertEquals(event, link.event)
        assertEquals(token, link.token)
        assertNotNull(link.createdAt)
    }

    @Test
    fun `create invite link accepts a token of the minimum length`() {
        val token = "a".repeat(InviteLink.TOKEN_MIN_LENGTH)

        val link = InviteLink.create(event = event, token = token)

        assertEquals(token, link.token)
    }

    @Test
    fun `create invite link fails with blank token`() {
        assertFailsWith<IllegalArgumentException> {
            InviteLink.create(event = event, token = "   ")
        }
    }

    @Test
    fun `create invite link fails with a token shorter than the minimum`() {
        assertFailsWith<IllegalArgumentException> {
            InviteLink.create(event = event, token = "a".repeat(InviteLink.TOKEN_MIN_LENGTH - 1))
        }
    }

    @Test
    fun `create invite link fails with a token longer than the maximum`() {
        assertFailsWith<IllegalArgumentException> {
            InviteLink.create(event = event, token = "a".repeat(InviteLink.TOKEN_MAX_LENGTH + 1))
        }
    }
}
