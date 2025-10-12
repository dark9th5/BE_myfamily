package com.lc9th5.myfamily.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.*
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@Configuration
class KeysConfig {

    @Bean
    fun rsaKey(): RSAKey {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp: KeyPair = kpg.genKeyPair()
        val publicKey = kp.public as RSAPublicKey
        val privateKey = kp.private as RSAPrivateKey
        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
    }

    @Bean
    fun jwkSource(rsaKey: RSAKey) = ImmutableJWKSet<SecurityContext>(JWKSet(rsaKey))

    @Bean
    fun jwtEncoder(jwkSource: ImmutableJWKSet<SecurityContext>): JwtEncoder =
        NimbusJwtEncoder(jwkSource)

    @Bean
    fun jwtDecoder(
        rsaKey: RSAKey,
        @Value("\${security.jwt.issuer}") issuer: String
    ): JwtDecoder {
        val decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build()
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer))
        return decoder
    }
}