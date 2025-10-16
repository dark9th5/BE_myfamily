package com.lc9th5.myfamily.controller

import com.lc9th5.myfamily.auth.dto.AuthResponse
import com.lc9th5.myfamily.auth.dto.LoginRequest
import com.lc9th5.myfamily.auth.dto.RegisterRequest
import com.lc9th5.myfamily.auth.dto.VerifyRequest
import com.lc9th5.myfamily.service.TokenService
import com.lc9th5.myfamily.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val tokenService: TokenService
) {

    @PostMapping("/register")
    fun register(@RequestBody @Valid body: RegisterRequest): ResponseEntity<AuthResponse> {
        val user = userService.register(
            body.username,
            body.email,
            body.password,
            body.fullName
        )
        // Tạo token và hiệu lực cho user mới đăng ký   
        val (token, expiresIn) = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)
        val resp = AuthResponse(
            accessToken = token,
            refreshToken = refreshToken,
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
    fun login(@RequestBody @Valid body: LoginRequest): ResponseEntity<Any> {
        val authToken = UsernamePasswordAuthenticationToken(body.email.trim().lowercase(), body.password)
        val authentication = authenticationManager.authenticate(authToken)

        val user = userService.findByEmail(authentication.name)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (!user.isVerified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Tài khoản chưa được xác thực. Vui lòng kiểm tra email và xác thực tài khoản."))
        }

        val (token, expiresIn) = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)

        return ResponseEntity.ok(
            AuthResponse(
                accessToken = token,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                user = AuthResponse.UserInfo(
                    id = user.id,
                    email = user.email,
                    fullName = user.fullName
                )
            )
        )
    }

    @PostMapping("/refresh")
    fun refresh(@RequestHeader("Authorization") authHeader: String): ResponseEntity<AuthResponse> {
        if (!authHeader.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header")
        }
        val refreshToken = authHeader.substring(7)

        val result = tokenService.generateAccessTokenFromRefreshToken(refreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token")

        val (newAccessToken, expiresIn) = result

        // Get user from stored refresh token
        val storedToken = tokenService.getRefreshTokenEntity(refreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found")

        return ResponseEntity.ok(
            AuthResponse(
                accessToken = newAccessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                user = AuthResponse.UserInfo(
                    id = storedToken.user.id,
                    email = storedToken.user.email,
                    fullName = storedToken.user.fullName
                )
            )
        )
    }

    @PostMapping("/verify")
    fun verify(@RequestBody @Valid body: VerifyRequest): ResponseEntity<Map<String, String>> {
        val success = userService.verifyUser(body.email, body.code)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "Tài khoản đã được xác thực thành công"))
        } else {
            ResponseEntity.badRequest().body(mapOf("error" to "Mã xác thực không hợp lệ hoặc email chưa đăng ký"))
        }
    }
}