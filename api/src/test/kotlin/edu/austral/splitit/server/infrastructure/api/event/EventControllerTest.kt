package edu.austral.splitit.server.infrastructure.api.event

import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.CreateEventCommand
import edu.austral.splitit.server.application.service.EventApplicationService
import edu.austral.splitit.server.application.service.EventSummary
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals

@WebMvcTest(
    controllers = [EventController::class],
    excludeAutoConfiguration = [
        UserDetailsServiceAutoConfiguration::class,
    ],
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "jwt.expiration-hours=8",
        "jwt.secret=test-jwt-secret-that-is-long-enough",
    ],
)
class EventControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var eventApplicationService: EventApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    private val authUser =
        AuthUser(
            id = 1L,
            username = "mateo@example.com",
            password = "",
            roles = emptyList(),
            name = "Mateo",
        )

    @Test
    fun `post creates an event and returns 201 with summary`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.createEvent(any())).thenReturn(
            EventSummary(
                id = 1L,
                name = "Viaje a Bariloche",
                description = "Vacaciones de verano",
                iconKey = "plane",
                baseCurrency = "ARS",
                memberCount = 3L,
            ),
        )

        mockMvc
            .perform(
                post("/api/events")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Viaje a Bariloche",
                          "description": "Vacaciones de verano",
                          "iconKey": "plane",
                          "baseCurrency": "ARS",
                          "participantNames": ["Ana", "Juan"]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Viaje a Bariloche"))
            .andExpect(jsonPath("$.description").value("Vacaciones de verano"))
            .andExpect(jsonPath("$.iconKey").value("plane"))
            .andExpect(jsonPath("$.baseCurrency").value("ARS"))
            .andExpect(jsonPath("$.memberCount").value(3))

        verify(eventApplicationService).createEvent(
            check<CreateEventCommand> {
                assertEquals(1L, it.userId)
                assertEquals("Viaje a Bariloche", it.name)
                assertEquals("Vacaciones de verano", it.description)
                assertEquals("plane", it.iconKey)
                assertEquals("ARS", it.baseCurrency)
                assertEquals(listOf("Ana", "Juan"), it.participantNames)
            },
        )
    }

    @Test
    fun `post returns 401 without auth cookie`() {
        mockMvc
            .perform(
                post("/api/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Viaje a Bariloche",
                          "baseCurrency": "ARS"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(eventApplicationService, never()).createEvent(any())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"baseCurrency":"ARS"}""",
            """{"name":"","baseCurrency":"ARS"}""",
            """{"name":"   ","baseCurrency":"ARS"}""",
            """{"name":"Viaje","baseCurrency":""}""",
            """{"name":"Viaje","baseCurrency":"AR"}""",
            """{"name":"Viaje","baseCurrency":"PESOS"}""",
        ],
    )
    fun `post returns 400 for invalid body`(body: String) {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)

        mockMvc
            .perform(
                post("/api/events")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(eventApplicationService, never()).createEvent(any())
    }

    @Test
    fun `get returns list of user events with 200`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listUserEvents(1L)).thenReturn(
            listOf(
                EventSummary(
                    id = 1L,
                    name = "Viaje a Bariloche",
                    description = "Vacaciones de verano",
                    iconKey = "plane",
                    baseCurrency = "ARS",
                    memberCount = 3L,
                ),
            ),
        )

        mockMvc
            .perform(
                get("/api/events")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Viaje a Bariloche"))
            .andExpect(jsonPath("$[0].description").value("Vacaciones de verano"))
            .andExpect(jsonPath("$[0].iconKey").value("plane"))
            .andExpect(jsonPath("$[0].baseCurrency").value("ARS"))
            .andExpect(jsonPath("$[0].memberCount").value(3))

        verify(eventApplicationService).listUserEvents(1L)
    }

    @Test
    fun `get returns empty list with 200 when user has no events`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(eventApplicationService.listUserEvents(1L)).thenReturn(emptyList())

        mockMvc
            .perform(
                get("/api/events")
                    .cookie(Cookie("auth_token", "good-token")),
            ).andExpect(status().isOk)
            .andExpect(content().json("[]"))

        verify(eventApplicationService).listUserEvents(1L)
    }

    @Test
    fun `get returns 401 without auth cookie`() {
        mockMvc
            .perform(get("/api/events"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(eventApplicationService, never()).listUserEvents(any())
    }
}
