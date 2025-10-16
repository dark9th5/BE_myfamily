package com.lc9th5.myfamily.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
data class JwtProperties(
    val secret: String = System.getenv("JWT_SECRET") ?: throw IllegalArgumentException("JWT_SECRET environment variable is required"),
    val expirationSeconds: Long = System.getenv("JWT_EXPIRATION")?.toLong() ?: 7200,
    val refreshExpirationSeconds: Long = System.getenv("JWT_REFRESH_EXPIRATION")?.toLong() ?: 604800 // 7 days
)