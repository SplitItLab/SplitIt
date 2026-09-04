package edu.austral.splitit.server.domain.service

import edu.austral.splitit.server.domain.model.event.Event
import edu.austral.splitit.server.domain.model.event.EventMember
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.infrastructure.persistence.EventMemberCountProjection
import edu.austral.splitit.server.infrastructure.persistence.EventMemberRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class EventMemberServiceTest {
    private val eventMemberRepository: EventMemberRepository = mock()
    private val eventMemberService = EventMemberService(eventMemberRepository)

    private val owner = User(id = 1L, name = "Dueño", email = "dueno@example.com", passwordHash = "hash")
    private val event = Event.create(owner = owner, name = "Viaje", baseCurrency = "ARS")

    @Test
    fun `addMember saves and returns member`() {
        whenever(eventMemberRepository.save(any<EventMember>())).thenAnswer { invocation ->
            val m = invocation.getArgument<EventMember>(0)
            m.id = 5L
            m
        }

        val member = eventMemberService.addMember(event = event, displayName = "Dueño", user = owner)

        assertEquals(5L, member.id)
        assertEquals("Dueño", member.displayName)
        assertEquals(owner, member.user)
    }

    @Test
    fun `addMembers saves all and returns members`() {
        whenever(eventMemberRepository.saveAll(any<Iterable<EventMember>>())).thenAnswer { invocation ->
            val members = invocation.getArgument<Iterable<EventMember>>(0).toList()
            members.forEachIndexed { index, m -> m.id = (index + 1).toLong() }
            members
        }

        val members = eventMemberService.addMembers(event, listOf("Ana", "Juan"))

        assertEquals(2, members.size)
        assertEquals("Ana", members[0].displayName)
        assertEquals("Juan", members[1].displayName)

        val captor = argumentCaptor<Iterable<EventMember>>()
        verify(eventMemberRepository).saveAll(captor.capture())
        assertEquals(2, captor.firstValue.toList().size)
    }

    @Test
    fun `countByEventId delegates to repository`() {
        whenever(eventMemberRepository.countByEventId(1L)).thenReturn(3L)

        val count = eventMemberService.countByEventId(1L)
        assertEquals(3L, count)
    }

    @Test
    fun `findByEventId delegates to repository`() {
        val member = EventMember.create(event, "Ana")
        whenever(eventMemberRepository.findAllByEventId(1L)).thenReturn(listOf(member))

        val members = eventMemberService.findByEventId(1L)
        assertEquals(1, members.size)
        assertEquals("Ana", members[0].displayName)
    }

    @Test
    fun `getMemberCounts maps projections to map`() {
        val proj1 =
            object : EventMemberCountProjection {
                override val eventId = 1L
                override val memberCount = 3L
            }
        val proj2 =
            object : EventMemberCountProjection {
                override val eventId = 2L
                override val memberCount = 5L
            }

        whenever(eventMemberRepository.countMembersByEventIds(listOf(1L, 2L))).thenReturn(listOf(proj1, proj2))

        val map = eventMemberService.getMemberCounts(listOf(1L, 2L))
        assertEquals(2, map.size)
        assertEquals(3L, map[1L])
        assertEquals(5L, map[2L])
    }

    @Test
    fun `getMemberCounts returns empty map when eventIds is empty`() {
        val map = eventMemberService.getMemberCounts(emptyList())
        assertEquals(emptyMap(), map)
    }
}
