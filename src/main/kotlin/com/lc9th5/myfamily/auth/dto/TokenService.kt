package com.lc9th5.myfamily.auth

import com.lc9th5.myfamily.model.user.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${security.jwt.expiration-seconds:3600}") private val expirationSeconds: Long
) {
    fun generateAccessToken(user: User): Pair<String, Long> {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(expirationSeconds)

        val scope = user.roles.joinToString(" ") { "ROLE_${it.name}" }

        val claims = JwtClaimsSet.builder()
            .issuer("myfamily-api")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.email)
            .claim("uid", user.id)
            .claim("name", user.fullName)
            .claim("scope", scope)
            .build()

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode { it.headers { h -> h.headers(jwsHeader.headers) }.claims { _ -> claims } }.tokenValue
        val ttl = expirationSeconds
        return token to ttl
    }
}