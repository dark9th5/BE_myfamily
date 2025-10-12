package com.lc9th5.myfamily.auth.social

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

data class GoogleProfile(
    val subject: String,
    val email: String?,
    val name: String?,
    val picture: String?
)

@Service
class GoogleTokenVerifierService(
    @Value("\${social.google.clientId}") private val clientId: String
) {
    private val transport = NetHttpTransport()
    private val jsonFactory = JacksonFactory.getDefaultInstance()

    fun verify(idTokenString: String): GoogleProfile? {
        val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
            .setAudience(listOf(clientId))
            .build()

        val idToken: GoogleIdToken = verifier.verify(idTokenString) ?: return null
        val payload = idToken.payload

        return GoogleProfile(
            subject = payload.subject,
            email = payload.email,
            name = payload["name"] as? String,
            picture = payload["picture"] as? String
        )
    }
}