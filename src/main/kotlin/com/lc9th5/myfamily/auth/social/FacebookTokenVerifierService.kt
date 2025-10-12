package com.lc9th5.myfamily.auth.social

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class FacebookProfile(
    val id: String,
    val name: String?,
    val email: String?,
    val picture: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DebugTokenData(
    val app_id: String?,
    val is_valid: Boolean?,
    val user_id: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DebugTokenResponse(
    val data: DebugTokenData?
)

@Service
class FacebookTokenVerifierService(
    @Value("\${social.facebook.appId}") private val appId: String,
    @Value("\${social.facebook.appSecret}") private val appSecret: String
) {
    private val client = HttpClient.newHttpClient()
    private val mapper = jacksonObjectMapper()

    fun verify(userAccessToken: String): FacebookProfile? {
        // 1) Validate token via debug_token
        val appToken = "$appId|$appSecret"
        val debugUri = URI.create(
            "https://graph.facebook.com/debug_token?input_token=$userAccessToken&access_token=$appToken"
        )
        val debugReq = HttpRequest.newBuilder(debugUri).GET().build()
        val debugRes = client.send(debugReq, HttpResponse.BodyHandlers.ofString())
        if (debugRes.statusCode() !in 200..299) return null

        val debug: DebugTokenResponse = mapper.readValue(debugRes.body())
        val data = debug.data ?: return null
        if (data.is_valid != true || data.app_id != appId || data.user_id.isNullOrBlank()) return null

        // 2) Fetch profile with appsecret_proof
        val proof = hmacSha256(userAccessToken, appSecret)
        val meUri = URI.create(
            "https://graph.facebook.com/v19.0/me?fields=id,name,email,picture.type(large)&access_token=$userAccessToken&appsecret_proof=$proof"
        )
        val meReq = HttpRequest.newBuilder(meUri).GET().build()
        val meRes = client.send(meReq, HttpResponse.BodyHandlers.ofString())
        if (meRes.statusCode() !in 200..299) {
            // fallback: still allow token if debug was valid (minimal)
            return FacebookProfile(id = data.user_id!!, name = null, email = null, picture = null)
        }

        val node = mapper.readTree(meRes.body())
        val id = node.get("id")?.asText()
        val name = node.get("name")?.asText()
        val email = node.get("email")?.asText()
        val pictureUrl = node.path("picture").path("data").get("url")?.asText()

        return if (!id.isNullOrBlank()) FacebookProfile(id, name, email, pictureUrl) else null
    }

    private fun hmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}