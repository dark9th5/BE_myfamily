package com.lc9th5.myfamily.auth

import com.lc9th5.myfamily.security.JwtTokenService
import com.lc9th5.myfamily.security.TokenResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class RefreshRequest(
    @field:NotBlank val refresh_token: String
)

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenService: JwtTokenService,
    private val jwtDecoder: JwtDecoder,
    private val userDetailsService: UserDetailsService,
    @Value("\${security.jwt.issuer}") private val issuer: String
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): TokenResponse {
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(req.username, req.password)
        )
        return jwtTokenService.generateTokens(auth)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: RefreshRequest): TokenResponse {
        try {
            val jwt = jwtDecoder.decode(req.refresh_token)

            val type = jwt.claims["type"] as? String
            if (type != "refresh_token") {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token type")
            }

            // issuer đã được validator của JwtDecoder kiểm tra. Có thể kiểm lại nếu muốn.
            val username = jwt.subject ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject")

            // Tải lại user và phát hành token mới (rotation)
            val user = userDetailsService.loadUserByUsername(username)
            val auth = UsernamePasswordAuthenticationToken(user.username, null, user.authorities)

            return jwtTokenService.generateTokens(auth)
        } catch (ex: JwtException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token")
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot refresh token")
        }
    }
}