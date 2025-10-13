package com.lc9th5.myfamily.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(
    private val jwtProperties: JwtProperties
) {

    private fun secretKeySpec(): SecretKeySpec {
        // Secret dạng chuỗi; bạn có thể để dạng Base64 và decode nếu muốn
        val keyBytes = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        return SecretKeySpec(keyBytes, "HmacSHA256")
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder
            .withSecretKey(secretKeySpec())
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }

    @Bean
    fun jwtEncoder(): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(secretKeySpec()).build()
        val jwkSet = JWKSet(jwk)
        val jwkSource = com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> { selector, _ ->
            selector.select(jwkSet)
        }
        return NimbusJwtEncoder(jwkSource)
    }
}