package com.lc9th5.myfamily.service

import com.lc9th5.myfamily.model.user.Role
import com.lc9th5.myfamily.model.user.User
import com.lc9th5.myfamily.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {
    @Transactional
    fun register(username: String, email: String, rawPassword: String, fullName: String?): User {
        if (userRepository.existsByEmail(email.trim().lowercase())) {
            throw IllegalArgumentException("Email already registered")
        }
        val verificationCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val user = User(
            username = username.trim(),
            email = email.trim().lowercase(),
            password = passwordEncoder.encode(rawPassword),
            fullName = fullName?.trim(),
            roles = mutableSetOf(Role.USER),
            isVerified = false,
            verificationCode = verificationCode
        )
        val savedUser = userRepository.save(user)
        try {
            emailService.sendVerificationEmail(savedUser.email, verificationCode)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to send verification email: ${e.message}")
        }
        return savedUser
    }

    fun findByEmail(email: String): User? =
        userRepository.findByEmail(email.trim().lowercase())

    @Transactional
    fun verifyUser(email: String, code: String): Boolean {
        val user = userRepository.findByEmail(email.trim().lowercase())
            ?: return false
        if (user.verificationCode == code.uppercase() && !user.isVerified) {
            user.isVerified = true
            user.verificationCode = null // Clear code after verify
            userRepository.save(user)
            return true
        }
        return false
    }
}