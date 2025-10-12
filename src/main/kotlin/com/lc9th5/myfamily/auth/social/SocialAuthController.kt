package com.lc9th5.myfamily.auth.social

import com.lc9th5.myfamily.security.JwtTokenService
import com.lc9th5.myfamily.security.TokenResponse
import com.lc9th5.myfamily.auth.social.FacebookTokenVerifierService
import com.lc9th5.myfamily.auth.social.GoogleTokenVerifierService
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class GoogleLoginRequest(@field:NotBlank val id_token: String)
data class FacebookLoginRequest(@field:NotBlank val access_token: String)

@RestController
@RequestMapping("/auth/social")
class SocialAuthController(
    private val googleVerifier: GoogleTokenVerifierService,
    private val facebookVerifier: FacebookTokenVerifierService,
    private val jwtTokenService: JwtTokenService
) {

    @PostMapping("/google")
    fun google(@RequestBody req: GoogleLoginRequest): TokenResponse {
        val profile = googleVerifier.verify(req.id_token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token")

        // TODO: upsert user in DB by profile.email (nếu có) hoặc profile.sub
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val auth = UsernamePasswordAuthenticationToken(profile.subject, null, authorities)
        return jwtTokenService.generateTokens(auth)
    }

    @PostMapping("/facebook")
    fun facebook(@RequestBody req: FacebookLoginRequest): TokenResponse {
        val profile = facebookVerifier.verify(req.access_token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Facebook token")

        // TODO: upsert user in DB by profile.email (nếu có) hoặc profile.id
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val auth = UsernamePasswordAuthenticationToken(profile.id, null, authorities)
        return jwtTokenService.generateTokens(auth)
    }
}