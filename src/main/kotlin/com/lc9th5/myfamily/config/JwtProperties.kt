package com.lc9th5.myfamily.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties(
    var secret: String = "",
    var expirationSeconds: Long = 3600
)