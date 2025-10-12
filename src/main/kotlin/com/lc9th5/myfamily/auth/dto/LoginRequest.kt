package com.lc9th5.myfamily.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:Email @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String
)