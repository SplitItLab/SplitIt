package edu.austral.splitit.server.domain.model.event

import edu.austral.splitit.server.Helpers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventTest {
    private val owner = Helpers.user(name = "Dueño", email = "dueno@example.com", passwordHash = "hash")

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

    @Test
    fun `create fails when currency is not three letters`() {
        assertFailsWith<IllegalArgumentException> {
            Event.create(
                owner = owner,
                name = "Viaje",
                baseCurrency = "123",
            )
        }
    }

    @Test
    fun `update applies provided fields and updates updatedAt`() {
        val event =
            Event.create(
                owner = owner,
                name = "Original",
                description = "Desc original",
                iconKey = "bus",
                baseCurrency = "ARS",
            )
        val previousUpdatedAt = event.updatedAt

        Thread.sleep(1)
        event.update(
            name = "  Nuevo nombre  ",
            description = "  Nueva descripción  ",
            iconKey = " car ",
        )

        assertEquals("Nuevo nombre", event.name)
        assertEquals("Nueva descripción", event.description)
        assertEquals("car", event.iconKey)
        assertTrue(event.updatedAt.isAfter(previousUpdatedAt))
    }

    @Test
    fun `update sets null for blank optional fields`() {
        val event =
            Event.create(
                owner = owner,
                name = "Original",
                description = "Desc",
                iconKey = "bus",
                baseCurrency = "ARS",
            )

        event.update(description = "   ", iconKey = "")

        assertNull(event.description)
        assertNull(event.iconKey)
    }

    @Test
    fun `update leaves unchanged fields when not provided`() {
        val event =
            Event.create(
                owner = owner,
                name = "Original",
                description = "Desc",
                iconKey = "bus",
                baseCurrency = "ARS",
            )

        event.update(name = "Nuevo")

        assertEquals("Nuevo", event.name)
        assertEquals("Desc", event.description)
        assertEquals("bus", event.iconKey)
    }

    @Test
    fun `update fails when name is blank`() {
        val event = Event.create(owner = owner, name = "Válido", baseCurrency = "ARS")

        assertFailsWith<IllegalArgumentException> {
            event.update(name = "   ")
        }
    }
}
