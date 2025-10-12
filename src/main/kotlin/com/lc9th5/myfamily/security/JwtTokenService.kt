package com.lc9th5.myfamily.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "Bearer",
    val expires_in: Long
)

@Service
class JwtTokenService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${security.jwt.issuer}") private val issuer: String,
    @Value("\${security.jwt.access-token-ttl-seconds:900}") private val accessTtlSeconds: Long,
    @Value("\${security.jwt.refresh-token-ttl-seconds:2592000}") private val refreshTtlSeconds: Long,
) {

    fun generateTokens(auth: Authentication): TokenResponse {
        val now = Instant.now()
        val accessExpiresAt = now.plus(accessTtlSeconds, ChronoUnit.SECONDS)
        val refreshExpiresAt = now.plus(refreshTtlSeconds, ChronoUnit.SECONDS)

        val accessClaims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(accessExpiresAt)
            .subject(auth.name)
            .claim("scope", "api")
            .claim("username", auth.name)
            .build()

        val refreshClaims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(refreshExpiresAt)
            .subject(auth.name)
            .claim("type", "refresh_token")
            .build()

        val accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).tokenValue
        val refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(refreshClaims)).tokenValue

        return TokenResponse(
            access_token = accessToken,
            refresh_token = refreshToken,
            expires_in = accessTtlSeconds
        )
    }
}