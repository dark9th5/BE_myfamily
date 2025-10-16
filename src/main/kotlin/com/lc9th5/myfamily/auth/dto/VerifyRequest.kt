package com.lc9th5.myfamily.auth.dto

data class VerifyRequest(
    val email: String,
    val code: String
)