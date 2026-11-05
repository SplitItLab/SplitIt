package edu.austral.splitit.server.application.port

data class GoogleIdentity(
    val subject: String,
    val email: String,
    val name: String?,
)

interface GoogleTokenVerifier {
    // Null when the credential is not a valid, current Google ID token for this application.
    fun verify(credential: String): GoogleIdentity?
}
