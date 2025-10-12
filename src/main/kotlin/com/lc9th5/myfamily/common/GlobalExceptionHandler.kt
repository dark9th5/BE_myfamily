package com.lc9th5.myfamily.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

data class ApiError(val status: Int, val error: String, val message: String?)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException) =
        ResponseEntity(ApiError(401, "UNAUTHORIZED", ex.message), HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException) =
        ResponseEntity(ApiError(403, "FORBIDDEN", ex.message), HttpStatus.FORBIDDEN)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val msg = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity(ApiError(400, "BAD_REQUEST", msg), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleRse(ex: ResponseStatusException): ResponseEntity<ApiError> {
        val status = ex.statusCode.value()
        // HttpStatusCode không có reasonPhrase; dùng chuỗi của statusCode hoặc suy ra từ HttpStatus
        val errorText = (HttpStatus.resolve(status)?.reasonPhrase) ?: ex.statusCode.toString()
        return ResponseEntity(ApiError(status, errorText, ex.reason), ex.statusCode)
    }

    @ExceptionHandler(Exception::class)
    fun handleOther(ex: Exception) =
        ResponseEntity(ApiError(500, "INTERNAL_SERVER_ERROR", ex.message), HttpStatus.INTERNAL_SERVER_ERROR)
}