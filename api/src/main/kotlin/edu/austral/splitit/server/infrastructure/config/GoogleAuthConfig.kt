package edu.austral.splitit.server.infrastructure.config

import edu.austral.splitit.server.application.port.GoogleTokenVerifier
import edu.austral.splitit.server.infrastructure.security.GoogleIdTokenVerifier
import edu.austral.splitit.server.infrastructure.security.googleTokenValidator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

private const val GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"

@Configuration
class GoogleAuthConfig {
    @Bean
    fun googleTokenVerifier(
        @Value("\${google.client-id}") clientId: String,
    ): GoogleTokenVerifier {
        if (clientId.isBlank()) logger.warn("GOOGLE_CLIENT_ID is not set: signing in with Google is disabled")
        val decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build()
        decoder.setJwtValidator(googleTokenValidator(clientId))
        return GoogleIdTokenVerifier(decoder, clientId)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GoogleAuthConfig::class.java)
    }
}
