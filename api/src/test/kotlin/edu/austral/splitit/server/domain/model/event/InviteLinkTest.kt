package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.user.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class InviteLinkTest {
    private val owner = User(id = 1L, name = "Dueño", email = "dueno@example.com", passwordHash = "hash")
    private val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")

    @Test
    fun `create invite link successfully`() {
        val link = InviteLink.create(event = event, token = "opaque-token-12345")

        assertEquals(event, link.event)
        assertEquals("opaque-token-12345", link.token)
        assertNotNull(link.createdAt)
    }

    @Test
    fun `create invite link fails with blank token`() {
        assertFailsWith<IllegalArgumentException> {
            InviteLink.create(event = event, token = "   ")
        }
    }
}
