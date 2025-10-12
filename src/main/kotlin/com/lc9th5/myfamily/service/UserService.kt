package com.lc9th5.myfamily.service

import com.lc9th5.myfamily.model.User
import com.lc9th5.myfamily.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun registerUser(username: String, email: String, password: String, fullName: String?): User {
        if (userRepository.findByUsername(username) != null) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("Email already exists")
        }
        val encodedPassword = passwordEncoder.encode(password)// mã hóa
        val user = User(
            username = username,
            email = email,
            password = encodedPassword,
            fullName = fullName
        )
        return userRepository.save(user)
    }
}