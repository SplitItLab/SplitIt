package edu.austral.splitit.server.infrastructure.api.invitation

import edu.austral.splitit.server.application.exception.AccountAlreadyLinkedException
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.application.port.AuthUser
import edu.austral.splitit.server.application.port.TokenProvider
import edu.austral.splitit.server.application.service.GuestAccessService
import edu.austral.splitit.server.application.service.InvitationAccess
import edu.austral.splitit.server.application.service.InvitationApplicationService
import edu.austral.splitit.server.application.service.InvitationMemberSummary
import edu.austral.splitit.server.application.service.InvitationSummary
import edu.austral.splitit.server.application.service.LinkMemberCommand
import edu.austral.splitit.server.application.service.LinkedMemberSummary
import edu.austral.splitit.server.domain.model.event.MemberAlreadyLinkedException
import edu.austral.splitit.server.infrastructure.api.GlobalExceptionHandler
import edu.austral.splitit.server.infrastructure.config.SecurityConfig
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [InvitationController::class],
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
class InvitationControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var invitationApplicationService: InvitationApplicationService

    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @MockitoBean
    private lateinit var guestAccessService: GuestAccessService

    private val authUser =
        AuthUser(
            id = 2L,
            username = "sofia@example.com",
            password = "",
            roles = emptyList(),
            name = "Sofia",
        )

    @Test
    fun `get invitation returns 200 with the event basic data without session`() {
        whenever(invitationApplicationService.getInvitation(TOKEN)).thenReturn(
            InvitationSummary(
                name = "Juntada Asado",
                description = "Asado del sábado",
                iconKey = "food",
                memberCount = 4L,
            ),
        )

        mockMvc
            .perform(get("/api/invitations/$TOKEN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Juntada Asado"))
            .andExpect(jsonPath("$.description").value("Asado del sábado"))
            .andExpect(jsonPath("$.iconKey").value("food"))
            .andExpect(jsonPath("$.memberCount").value(4))
            .andExpect(jsonPath("$.id").doesNotExist())
    }

    @Test
    fun `get invitation returns 404 with the generic message for an invalid token`() {
        whenever(invitationApplicationService.getInvitation("token-invalido")).thenThrow(EventNotFoundException())

        mockMvc
            .perform(get("/api/invitations/token-invalido"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }

    @Test
    fun `get invitation members returns 200 marking linked members without session`() {
        whenever(invitationApplicationService.listMembers(TOKEN)).thenReturn(
            listOf(
                InvitationMemberSummary(id = 1L, name = "Nicolas", isLinked = true),
                InvitationMemberSummary(id = 2L, name = "Francisco", isLinked = false),
            ),
        )

        mockMvc
            .perform(get("/api/invitations/$TOKEN/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Nicolas"))
            .andExpect(jsonPath("$[0].isLinked").value(true))
            .andExpect(jsonPath("$[0].email").doesNotExist())
            .andExpect(jsonPath("$[1].isLinked").value(false))
    }

    @Test
    fun `get invitation members returns 404 for an invalid token`() {
        whenever(invitationApplicationService.listMembers("token-invalido")).thenThrow(EventNotFoundException())

        mockMvc
            .perform(get("/api/invitations/token-invalido/members"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }

    @Test
    fun `get access returns the linked member of the account`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(invitationApplicationService.getAccess(2L, TOKEN)).thenReturn(
            InvitationAccess(eventId = 10L, linkedMember = LinkedMemberSummary(id = 2L, name = "Francisco")),
        )

        mockMvc
            .perform(get("/api/invitations/$TOKEN/access").cookie(Cookie("auth_token", "good-token")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value(10))
            .andExpect(jsonPath("$.linkedMember.id").value(2))
            .andExpect(jsonPath("$.linkedMember.name").value("Francisco"))
    }

    @Test
    fun `get access returns 401 without session`() {
        mockMvc
            .perform(get("/api/invitations/$TOKEN/access"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized"))

        verify(invitationApplicationService, never()).getAccess(any(), any())
    }

    @Test
    fun `post link returns 200 and links the account with session`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(
            invitationApplicationService.linkMember(LinkMemberCommand(userId = 2L, token = TOKEN, memberId = 2L)),
        ).thenReturn(InvitationAccess(eventId = 10L, linkedMember = LinkedMemberSummary(id = 2L, name = "Francisco")))

        mockMvc
            .perform(
                post("/api/invitations/$TOKEN/link")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"memberId": 2}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value(10))
            .andExpect(jsonPath("$.linkedMember.id").value(2))
            .andExpect(jsonPath("$.linkedMember.name").value("Francisco"))
    }

    @Test
    fun `post link returns 409 when the member is already linked to another account`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(invitationApplicationService.linkMember(any())).thenThrow(MemberAlreadyLinkedException())

        mockMvc
            .perform(
                post("/api/invitations/$TOKEN/link")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"memberId": 1}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Member is already linked to an account"))
    }

    @Test
    fun `post link returns 409 when the account is already linked to the event`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(invitationApplicationService.linkMember(any())).thenThrow(AccountAlreadyLinkedException())

        mockMvc
            .perform(
                post("/api/invitations/$TOKEN/link")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"memberId": 2}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Account is already linked to this event"))
    }

    @Test
    fun `post link returns 401 without session`() {
        mockMvc
            .perform(
                post("/api/invitations/$TOKEN/link")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"memberId": 2}"""),
            ).andExpect(status().isUnauthorized)

        verify(invitationApplicationService, never()).linkMember(any())
    }

    @Test
    fun `post link returns 400 without a valid member id`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)

        mockMvc
            .perform(
                post("/api/invitations/$TOKEN/link")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"memberId": 0}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        verify(invitationApplicationService, never()).linkMember(any())
    }

    @Test
    fun `post link returns 404 for an invalid token`() {
        whenever(tokenProvider.parse("good-token")).thenReturn(authUser)
        whenever(invitationApplicationService.linkMember(any())).thenThrow(EventNotFoundException())

        mockMvc
            .perform(
                post("/api/invitations/token-invalido/link")
                    .cookie(Cookie("auth_token", "good-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"memberId": 2}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event not found"))
    }

    private companion object {
        const val TOKEN = "r7Qk2Vx9mLpZ4tHnCwB3sdAbCdEf"
    }
}
