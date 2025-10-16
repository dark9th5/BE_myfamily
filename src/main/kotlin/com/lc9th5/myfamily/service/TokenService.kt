package com.lc9th5.myfamily.service

import com.lc9th5.myfamily.config.JwtProperties
import com.lc9th5.myfamily.model.user.RefreshToken
import com.lc9th5.myfamily.model.user.User
import com.lc9th5.myfamily.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties,
    private val refreshTokenRepository: RefreshTokenRepository
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

    fun generateRefreshToken(user: User): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(jwtProperties.refreshExpirationSeconds)

        val claims = JwtClaimsSet.builder()
            .issuer("myfamily-api")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.email)
            .claim("uid", user.id)
            .build()

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        val params = JwtEncoderParameters.from(jwsHeader, claims)
        val token = jwtEncoder.encode(params).tokenValue

        // Save to database
        val refreshToken = RefreshToken(
            token = token,
            user = user,
            expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
        )
        refreshTokenRepository.save(refreshToken)

        return token
    }

    fun generateAccessTokenFromRefreshToken(refreshToken: String): Pair<String, Long>? {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: return null

        // Check if expired
        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken)
            return null
        }

        // Generate new access token
        return generateAccessToken(storedToken.user)
    }

    fun getRefreshTokenEntity(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token)
    }
}