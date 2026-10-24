package edu.austral.splitit.server.infrastructure.api.auth

import edu.austral.splitit.server.application.exception.InviteLinkNotFoundException
import edu.austral.splitit.server.application.exception.MemberHasAccountException
import edu.austral.splitit.server.application.exception.MemberNotInEventException
import edu.austral.splitit.server.application.port.AuthGuest
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import edu.austral.splitit.server.infrastructure.security.SessionCookieWriter
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [GuestSessionController::class],
    excludeAutoConfiguration = [
        UserDetailsServiceAutoConfiguration::class,
    ],
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class, SessionCookieWriter::class)
@TestPropertySource(
    properties = [
        "auth.cookie.name=auth_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "jwt.expiration-hours=8",
    ],
)
class GuestSessionControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `enter returns the guest and sets the guest cookie without a session`() {
        whenever(guestAccessService.enter(TOKEN, 30L)).thenReturn(juanGuest())
        whenever(tokenProvider.issueGuest(juanGuest())).thenReturn("guest-jwt")

        enter(TOKEN, """{"memberId":30}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("guest"))
            .andExpect(jsonPath("$.eventId").value(10))
            .andExpect(jsonPath("$.memberId").value(30))
            .andExpect(jsonPath("$.name").value("Juan"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(
                header().string(
                    HttpHeaders.SET_COOKIE,
                    allOf(
                        containsString("guest_token=guest-jwt"),
                        containsString("HttpOnly"),
                        containsString("Max-Age=604800"),
                    ),
                ),
            )
    }

    @Test
    fun `two guests can enter as the same member`() {
        whenever(guestAccessService.enter(TOKEN, 30L)).thenReturn(juanGuest())
        whenever(tokenProvider.issueGuest(juanGuest())).thenReturn("guest-jwt")

        enter(TOKEN, """{"memberId":30}""").andExpect(status().isOk)
        enter(TOKEN, """{"memberId":30}""").andExpect(status().isOk)
    }

    @Test
    fun `enter with an invalid link returns 404`() {
        whenever(guestAccessService.enter("roto", 30L)).thenThrow(InviteLinkNotFoundException())

        enter("roto", """{"memberId":30}""")
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Invite link not found"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
    }

    @Test
    fun `enter with a member linked to an account returns 409`() {
        whenever(guestAccessService.enter(TOKEN, 31L)).thenThrow(MemberHasAccountException())

        enter(TOKEN, """{"memberId":31}""")
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Member has an account"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
    }

    @Test
    fun `enter with a member of another event returns 422`() {
        whenever(guestAccessService.enter(TOKEN, 40L)).thenThrow(MemberNotInEventException())

        enter(TOKEN, """{"memberId":40}""")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.message").value("Member not in event"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
    }

    @Test
    fun `enter without a member returns 400`() {
        enter(TOKEN, "{}")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(guestAccessService, never()).enter(any(), any())
    }

    private fun enter(
        token: String,
        body: String,
    ): ResultActions =
        mockMvc.perform(
            post("/api/invite-links/$token/guest-session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    private fun juanGuest(): AuthGuest = AuthGuest(eventId = 10L, memberId = 30L, name = "Juan")

    private companion object {
        const val TOKEN = "un-token-de-invitacion-valido"
    }
}
