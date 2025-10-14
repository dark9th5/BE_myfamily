package com.lc9th5.myfamily.service

import com.lc9th5.myfamily.config.JwtProperties
import com.lc9th5.myfamily.model.user.User
import org.springframework.stereotype.Service
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import java.time.Instant

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties
) {
    fun generateAccessToken(user: User): Pair<String, Long> {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(jwtProperties.expirationSeconds)

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
        val params = JwtEncoderParameters.from(jwsHeader, claims)
        val token = jwtEncoder.encode(params).tokenValue
        val ttl = jwtProperties.expirationSeconds
        return token to ttl
    }
}