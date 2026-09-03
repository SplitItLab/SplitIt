package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.user.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventMemberTest {
    private val owner = User(id = 1L, name = "Dueño", email = "dueno@example.com", passwordHash = "hash")
    private val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")

    @Test
    fun `create member without linked user`() {
        val member = EventMember.create(event = event, displayName = "  Ana  ")

        assertEquals("Ana", member.displayName)
        assertEquals(event, member.event)
        assertNull(member.user)
        assertNotNull(member.joinedAt)
    }

    @Test
    fun `create member with linked user`() {
        val member = EventMember.create(event = event, displayName = "Dueño", user = owner)

        assertEquals("Dueño", member.displayName)
        assertEquals(owner, member.user)
    }

    @Test
    fun `link user to existing member`() {
        val member = EventMember.create(event = event, displayName = "Ana")
        assertNull(member.user)

        val user = User(id = 2L, name = "Ana", email = "ana@example.com", passwordHash = "hash")
        member.linkUser(user)

        assertEquals(user, member.user)
    }

    @Test
    fun `create member fails with empty display name`() {
        assertFailsWith<IllegalArgumentException> {
            EventMember.create(event = event, displayName = "   ")
        }
    }
}
