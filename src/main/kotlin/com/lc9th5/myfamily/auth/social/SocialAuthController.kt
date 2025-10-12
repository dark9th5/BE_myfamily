package com.lc9th5.myfamily.auth.social

import com.lc9th5.myfamily.model.User
import com.lc9th5.myfamily.repository.UserRepository
import com.lc9th5.myfamily.security.JwtTokenService
import com.lc9th5.myfamily.security.TokenResponse
import com.lc9th5.myfamily.auth.social.FacebookTokenVerifierService
import com.lc9th5.myfamily.auth.social.GoogleTokenVerifierService
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

data class GoogleLoginRequest(@field:NotBlank val id_token: String)
data class FacebookLoginRequest(@field:NotBlank val access_token: String)

@RestController
@RequestMapping("/auth/social")
class SocialAuthController(
    private val googleVerifier: GoogleTokenVerifierService,
    private val facebookVerifier: FacebookTokenVerifierService,
    private val jwtTokenService: JwtTokenService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/google")
    fun google(@RequestBody req: GoogleLoginRequest): TokenResponse {
        val profile = googleVerifier.verify(req.id_token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token")

        // Upsert user in DB: prefer email as username, fallback to "google:{sub}"
        val username = if (!profile.email.isNullOrBlank()) profile.email else "google:${profile.subject}"
        
        val user = if (!profile.email.isNullOrBlank()) {
            userRepository.findByEmail(profile.email) ?: userRepository.findByUsername(username)
        } else {
            userRepository.findByUsername(username)
        }
        
        val dbUser = user ?: run {
            // Create new user with random password (social login doesn't use password)
            val randomPassword = passwordEncoder.encode(UUID.randomUUID().toString())
            userRepository.save(
                User(
                    username = username,
                    email = profile.email ?: "google-${profile.subject}@social.local",
                    password = randomPassword,
                    fullName = profile.name
                )
            )
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val auth = UsernamePasswordAuthenticationToken(dbUser.username, null, authorities)
        return jwtTokenService.generateTokens(auth)
    }

    @PostMapping("/facebook")
    fun facebook(@RequestBody req: FacebookLoginRequest): TokenResponse {
        val profile = facebookVerifier.verify(req.access_token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Facebook token")

        // Upsert user in DB: prefer email as username, fallback to "facebook:{id}"
        val username = if (!profile.email.isNullOrBlank()) profile.email else "facebook:${profile.id}"
        
        val user = if (!profile.email.isNullOrBlank()) {
            userRepository.findByEmail(profile.email) ?: userRepository.findByUsername(username)
        } else {
            userRepository.findByUsername(username)
        }
        
        val dbUser = user ?: run {
            // Create new user with random password (social login doesn't use password)
            val randomPassword = passwordEncoder.encode(UUID.randomUUID().toString())
            userRepository.save(
                User(
                    username = username,
                    email = profile.email ?: "facebook-${profile.id}@social.local",
                    password = randomPassword,
                    fullName = profile.name
                )
            )
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val auth = UsernamePasswordAuthenticationToken(dbUser.username, null, authorities)
        return jwtTokenService.generateTokens(auth)
    }
}