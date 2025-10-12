package com.lc9th5.myfamily.exception

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorBody(val message: String, val errors: Map<String, String>? = null)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorBody> {
        val errors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody("Validation failed", errors))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraint(e: ConstraintViolationException): ResponseEntity<ErrorBody> {
        val errors = e.constraintViolations.associate { v -> v.propertyPath.toString() to (v.message ?: "invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody("Validation failed", errors))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArg(e: IllegalArgumentException): ResponseEntity<ErrorBody> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody(e.message ?: "Bad request"))
    }
}