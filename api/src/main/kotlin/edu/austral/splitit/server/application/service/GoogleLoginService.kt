package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.InvalidCredentialsException
import edu.austral.splitit.server.application.exception.PasswordAccountExistsException
import edu.austral.splitit.server.application.port.GoogleIdentity
import edu.austral.splitit.server.application.port.GoogleTokenVerifier
import edu.austral.splitit.server.domain.model.user.Email
import edu.austral.splitit.server.domain.model.user.User
import edu.austral.splitit.server.domain.service.EventTransferService
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

enum class ExistingAccountChoice {
    // Same account, which from now on signs in with Google.
    LINK,

    // New account that takes over the events; the old one is disabled.
    REPLACE,
}

@Service
class GoogleLoginService(
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val userService: UserService,
    private val eventTransferService: EventTransferService,
) {
    @Transactional
    fun login(
        credential: String,
        choice: ExistingAccountChoice?,
    ): User {
        val identity = googleTokenVerifier.verify(credential) ?: throw InvalidCredentialsException()

        return userService.findByGoogleSubject(identity.subject) ?: enterByEmail(identity, choice)
    }

    private fun enterByEmail(
        identity: GoogleIdentity,
        choice: ExistingAccountChoice?,
    ): User {
        val email = Email.create(identity.email).getOrNull() ?: throw InvalidCredentialsException()
        val existing =
            userService.findByEmail(email)
                ?: return userService.saveGoogle(googleName(identity), email, identity.subject)

        return resolveExisting(existing, identity, email, choice)
    }

    private fun resolveExisting(
        existing: User,
        identity: GoogleIdentity,
        email: Email,
        choice: ExistingAccountChoice?,
    ): User {
        // The email belongs to a different Google account, so it is not ours to hand over.
        if (existing.passwordHash == null) throw InvalidCredentialsException()

        return when (choice) {
            null -> throw PasswordAccountExistsException()
            ExistingAccountChoice.LINK -> userService.linkGoogle(existing, identity.subject)
            ExistingAccountChoice.REPLACE -> replace(existing, identity, email)
        }
    }

    private fun replace(
        existing: User,
        identity: GoogleIdentity,
        email: Email,
    ): User {
        // The old account goes first so its email is free for the new one.
        userService.disable(existing)
        val replacement = userService.saveGoogle(googleName(identity), email, identity.subject)
        eventTransferService.transferAll(existing, replacement)
        return replacement
    }

    // shortcut: an unusable Google name falls back to the email local part, which can also be too short.
    private fun googleName(identity: GoogleIdentity): String {
        val name = identity.name?.trim()?.take(User.NAME_MAX)
        if (name != null && name.length >= User.NAME_MIN) return name

        return identity.email.substringBefore("@").take(User.NAME_MAX)
    }
}
