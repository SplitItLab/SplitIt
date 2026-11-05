package edu.austral.splitit.server.infrastructure.security

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GoogleIdTokenVerifierTest {
    private val googleKeys = newKeyPair()
    private val verifier = verifierTrusting(googleKeys, CLIENT_ID)

    @Test
    fun `a token signed by Google for this client is accepted`() {
        val identity = verifier.verify(token())

        assertNotNull(identity)
        assertEquals("google-subject-1", identity.subject)
        assertEquals("ada@example.com", identity.email)
        assertEquals("Ada Lovelace", identity.name)
    }

    @Test
    fun `both issuer spellings Google uses are accepted`() {
        assertNotNull(verifier.verify(token(issuer = "accounts.google.com")))
    }

    @Test
    fun `a token signed with another key is rejected`() {
        assertNull(verifier.verify(token(signedWith = newKeyPair())))
    }

    @Test
    fun `a token issued for another client ID is rejected`() {
        assertNull(verifier.verify(token(audience = "another-client.apps.googleusercontent.com")))
    }

    @Test
    fun `a token from another issuer is rejected`() {
        assertNull(verifier.verify(token(issuer = "https://evil.example.com")))
    }

    @Test
    fun `an expired token is rejected`() {
        assertNull(verifier.verify(token(expiresAt = Instant.now().minusSeconds(3600))))
    }

    @Test
    fun `a token without email is rejected`() {
        assertNull(verifier.verify(token(email = null)))
    }

    @Test
    fun `garbage is rejected`() {
        assertNull(verifier.verify("not-a-jwt"))
    }

    @Test
    fun `nothing is accepted when the client ID is not configured`() {
        assertNull(verifierTrusting(googleKeys, "").verify(token()))
    }

    private fun verifierTrusting(
        keys: KeyPair,
        clientId: String,
    ): GoogleIdTokenVerifier {
        val decoder = NimbusJwtDecoder.withPublicKey(keys.public as RSAPublicKey).build()
        decoder.setJwtValidator(googleTokenValidator(clientId))
        return GoogleIdTokenVerifier(decoder, clientId)
    }

    private fun token(
        signedWith: KeyPair = googleKeys,
        issuer: String = "https://accounts.google.com",
        audience: String = CLIENT_ID,
        expiresAt: Instant = Instant.now().plusSeconds(3600),
        email: String? = "ada@example.com",
    ): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("google-subject-1")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiresAt))
                .claim("name", "Ada Lovelace")
                .apply { email?.let { claim("email", it) } }
                .build()
        val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims)
        jwt.sign(RSASSASigner(signedWith.private as RSAPrivateKey))
        return jwt.serialize()
    }

    private fun newKeyPair(): KeyPair =
        KeyPairGenerator
            .getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

    private companion object {
        const val CLIENT_ID = "our-client.apps.googleusercontent.com"
    }
}
