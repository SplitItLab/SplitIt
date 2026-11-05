package edu.austral.splitit.server.infrastructure.security

import edu.austral.splitit.server.application.port.GoogleIdentity
import edu.austral.splitit.server.application.port.GoogleTokenVerifier
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator

private val GOOGLE_ISSUERS = setOf("accounts.google.com", "https://accounts.google.com")

// Signature is checked by the decoder; this adds expiry, Google as issuer and our client ID as audience.
fun googleTokenValidator(clientId: String): OAuth2TokenValidator<Jwt> =
    OAuth2TokenValidator { jwt ->
        val invalid = OAuth2Error("invalid_token")
        val failure = OAuth2TokenValidatorResult.failure(invalid)
        when {
            // Not jwt.issuer: Google also uses a bare "accounts.google.com", which is not a URL.
            jwt.getClaimAsString("iss") !in GOOGLE_ISSUERS -> failure
            jwt.audience?.contains(clientId) != true -> failure
            else -> JwtTimestampValidator().validate(jwt)
        }
    }

class GoogleIdTokenVerifier(
    private val decoder: JwtDecoder,
    private val clientId: String,
) : GoogleTokenVerifier {
    override fun verify(credential: String): GoogleIdentity? =
        // Without a client ID (QA and previews) nothing can be valid for this application.
        if (clientId.isBlank()) null else decode(credential)?.let(::identityOf)

    private fun decode(credential: String): Jwt? =
        try {
            decoder.decode(credential)
        } catch (exception: JwtException) {
            logger.warn("Google credential rejected: {}", exception.message)
            null
        }

    private fun identityOf(jwt: Jwt): GoogleIdentity? {
        val subject = jwt.subject
        val email = jwt.getClaimAsString("email")
        return if (subject != null && email != null) {
            GoogleIdentity(subject = subject, email = email, name = jwt.getClaimAsString("name"))
        } else {
            null
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GoogleIdTokenVerifier::class.java)
    }
}
