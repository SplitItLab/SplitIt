package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.domain.model.user.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventTest {
    private val owner = User(id = 1L, name = "Dueño", email = "dueno@example.com", passwordHash = "hash")

    @Test
    fun `create normalizes name and uppercase currency`() {
        val event =
            Event.create(
                owner = owner,
                name = "  Viaje a Bariloche  ",
                description = "  Vacaciones  ",
                iconKey = " plane ",
                baseCurrency = "ars",
            )

        assertEquals("Viaje a Bariloche", event.name)
        assertEquals("Vacaciones", event.description)
        assertEquals("plane", event.iconKey)
        assertEquals("ARS", event.baseCurrency)
        assertEquals(owner, event.owner)
        assertNotNull(event.createdAt)
        assertNotNull(event.updatedAt)
    }

    @Test
    fun `create sets null for empty description and iconKey`() {
        val event =
            Event.create(
                owner = owner,
                name = "Asado",
                description = "   ",
                iconKey = "",
                baseCurrency = "USD",
            )

        assertNull(event.description)
        assertNull(event.iconKey)
    }

    @Test
    fun `create fails when name is blank`() {
        assertFailsWith<IllegalArgumentException> {
            Event.create(
                owner = owner,
                name = "   ",
                baseCurrency = "ARS",
            )
        }
    }

    @Test
    fun `create fails when currency is not 3 characters`() {
        assertFailsWith<IllegalArgumentException> {
            Event.create(
                owner = owner,
                name = "Viaje",
                baseCurrency = "AR",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Event.create(
                owner = owner,
                name = "Viaje",
                baseCurrency = "PESOS",
            )
        }
    }
}
