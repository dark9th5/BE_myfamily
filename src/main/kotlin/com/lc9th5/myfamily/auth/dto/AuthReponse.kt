package com.lc9th5.myfamily.auth.dto

data class AuthResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserInfo
) {
    data class UserInfo(
        val id: Long,
        val email: String,
        val fullName: String?
    )
}