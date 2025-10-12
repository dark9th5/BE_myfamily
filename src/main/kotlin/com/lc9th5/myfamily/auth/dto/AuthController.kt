package com.lc9th5.myfamily.auth

import com.lc9th5.myfamily.auth.dto.AuthResponse
import com.lc9th5.myfamily.auth.dto.LoginRequest
import com.lc9th5.myfamily.auth.dto.RegisterRequest
import com.lc9th5.myfamily.auth.dto.TokenService
import com.lc9th5.myfamily.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val tokenService: TokenService
) {

    @PostMapping("/register")
    fun register(@RequestBody @Valid body: RegisterRequest): ResponseEntity<AuthResponse> {
        val user = userService.register(body.email, body.password, body.fullName)
        val (token, expiresIn) = tokenService.generateAccessToken(user)
        val resp = AuthResponse(
            accessToken = token,
            expiresIn = expiresIn,
            user = AuthResponse.UserInfo(
                id = user.id,
                email = user.email,
                fullName = user.fullName
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(resp)
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid body: LoginRequest): ResponseEntity<AuthResponse> {
        val authToken = UsernamePasswordAuthenticationToken(body.email.trim().lowercase(), body.password)
        val authentication = authenticationManager.authenticate(authToken)

        val user = userService.findByEmail(authentication.name)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val (token, expiresIn) = tokenService.generateAccessToken(user)

        return ResponseEntity.ok(
            AuthResponse(
                accessToken = token,
                expiresIn = expiresIn,
                user = AuthResponse.UserInfo(
                    id = user.id,
                    email = user.email,
                    fullName = user.fullName
                )
            )
        )
    }
}