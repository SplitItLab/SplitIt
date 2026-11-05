package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.Helpers
import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.application.exception.PasswordAccountExistsException
import edu.austral.splitit.server.application.port.GoogleIdentity
import edu.austral.splitit.server.application.port.GoogleTokenVerifier
import edu.austral.splitit.server.domain.service.EventTransferService
import edu.austral.splitit.server.domain.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.InOrder
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class GoogleLoginServiceTest {
    private val googleTokenVerifier: GoogleTokenVerifier = mock()
    private val userService: UserService = mock()
    private val eventTransferService: EventTransferService = mock()

    private val service = GoogleLoginService(googleTokenVerifier, userService, eventTransferService)

    private val ada = Helpers.emailOf("ada@example.com")

    @Test
    fun `a new email creates the account with the name and email Google returns`() {
        givenGoogleIdentity(name = "Ada Google")
        val created = Helpers.googleUser(name = "Ada Google")
        whenever(userService.saveGoogle("Ada Google", ada, SUBJECT)).thenReturn(created)

        val result = service.login(CREDENTIAL, null)

        assertSame(created, result)
        verify(userService).saveGoogle("Ada Google", ada, SUBJECT)
    }

    @Test
    fun `a Google name that is not usable falls back to the email local part`() {
        givenGoogleIdentity(name = null)
        whenever(userService.saveGoogle(any(), any(), any())).thenReturn(Helpers.googleUser())

        service.login(CREDENTIAL, null)

        verify(userService).saveGoogle("ada", ada, SUBJECT)
    }

    @Test
    fun `an email that already signed in with Google enters its account`() {
        givenGoogleIdentity()
        val existing = Helpers.googleUser()
        whenever(userService.findByGoogleSubject(SUBJECT)).thenReturn(existing)

        val result = service.login(CREDENTIAL, null)

        assertSame(existing, result)
        verify(userService, never()).saveGoogle(any(), any(), any())
        verify(userService, never()).linkGoogle(any(), any())
    }

    @Test
    fun `the Google name never overwrites the name of an existing account`() {
        givenGoogleIdentity(name = "Another Name")
        val existing = Helpers.googleUser(name = "Ada Lovelace")
        whenever(userService.findByGoogleSubject(SUBJECT)).thenReturn(existing)

        val result = service.login(CREDENTIAL, null)

        assertEquals("Ada Lovelace", result.name)
    }

    @Test
    fun `an email with a password account asks to choose and writes nothing`() {
        givenGoogleIdentity()
        whenever(userService.findByEmail(ada)).thenReturn(Helpers.user())

        assertFailsWith<PasswordAccountExistsException> { service.login(CREDENTIAL, null) }

        verify(userService, never()).saveGoogle(any(), any(), any())
        verify(userService, never()).linkGoogle(any(), any())
        verify(userService, never()).disable(any())
    }

    @Test
    fun `linking keeps the same account and its name`() {
        givenGoogleIdentity(name = "Another Name")
        val existing = Helpers.user(name = "Ada Lovelace")
        whenever(userService.findByEmail(ada)).thenReturn(existing)
        whenever(userService.linkGoogle(existing, SUBJECT)).thenAnswer { existing.apply { linkGoogle(SUBJECT) } }

        val result = service.login(CREDENTIAL, ExistingAccountChoice.LINK)

        assertSame(existing, result)
        assertEquals("Ada Lovelace", result.name)
        assertEquals(SUBJECT, result.googleSubject)
        assertNull(result.passwordHash)
        verify(userService, never()).saveGoogle(any(), any(), any())
    }

    @Test
    fun `replacing disables the old account, creates a new one and moves its events`() {
        givenGoogleIdentity(name = "Ada Google")
        val existing = Helpers.user(id = 1L)
        val replacement = Helpers.googleUser(name = "Ada Google", id = 2L)
        whenever(userService.findByEmail(ada)).thenReturn(existing)
        whenever(userService.saveGoogle("Ada Google", ada, SUBJECT)).thenReturn(replacement)

        val result = service.login(CREDENTIAL, ExistingAccountChoice.REPLACE)

        assertSame(replacement, result)
        val order: InOrder = inOrder(userService, eventTransferService)
        // The old account goes first so its email is free for the new one.
        order.verify(userService).disable(existing)
        order.verify(userService).saveGoogle("Ada Google", ada, SUBJECT)
        order.verify(eventTransferService).transferAll(existing, replacement)
    }

    @Test
    fun `a failure while replacing propagates so the transaction rolls back`() {
        givenGoogleIdentity()
        val existing = Helpers.user(id = 1L)
        whenever(userService.findByEmail(ada)).thenReturn(existing)
        whenever(userService.saveGoogle(any(), any(), any())).thenReturn(Helpers.googleUser(id = 2L))
        whenever(eventTransferService.transferAll(any(), any())).thenThrow(IllegalStateException("boom"))

        assertFailsWith<IllegalStateException> { service.login(CREDENTIAL, ExistingAccountChoice.REPLACE) }
    }

    @Test
    fun `an email owned by another Google account is not handed over`() {
        givenGoogleIdentity()
        whenever(userService.findByEmail(ada)).thenReturn(Helpers.googleUser(googleSubject = "someone-else"))

        assertFailsWith<InvalidCredentialsException> { service.login(CREDENTIAL, ExistingAccountChoice.LINK) }

        verify(userService, never()).linkGoogle(any(), any())
    }

    @Test
    fun `an invalid credential is rejected before touching any account`() {
        whenever(googleTokenVerifier.verify(CREDENTIAL)).thenReturn(null)

        assertFailsWith<InvalidCredentialsException> { service.login(CREDENTIAL, ExistingAccountChoice.REPLACE) }

        verify(userService, never()).findByGoogleSubject(any())
        verify(userService, never()).findByEmail(any())
    }

    private fun givenGoogleIdentity(name: String? = "Ada Lovelace") {
        whenever(googleTokenVerifier.verify(CREDENTIAL)).thenReturn(GoogleIdentity(SUBJECT, "ada@example.com", name))
    }

    private companion object {
        const val CREDENTIAL = "google.id.token"
        const val SUBJECT = "google-subject-1"
    }
}
