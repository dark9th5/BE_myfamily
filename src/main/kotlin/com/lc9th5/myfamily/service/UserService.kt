package com.lc9th5.myfamily.service

import com.lc9th5.myfamily.model.user.Role
import com.lc9th5.myfamily.model.user.User
import com.lc9th5.myfamily.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun register(username: String, email: String, rawPassword: String, fullName: String?): User {
        if (userRepository.existsByEmail(email.trim().lowercase())) {
            throw IllegalArgumentException("Email already registered")
        }
        val user = User(
            username = username.trim(),
            email = email.trim().lowercase(),
            password = passwordEncoder.encode(rawPassword),
            fullName = fullName?.trim(),
            roles = mutableSetOf(Role.USER)
        )
        return userRepository.save(user)
    }

    fun findByEmail(email: String): User? =
        userRepository.findByEmail(email.trim().lowercase())
}