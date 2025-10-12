package com.lc9th5.myfamily.controller


import com.lc9th5.myfamily.model.User
import com.lc9th5.myfamily.service.UserService
import org.springframework.web.bind.annotation.*

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String?
)

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): User {
        return userService.registerUser(
            username = request.username,
            email = request.email,
            password = request.password,
            fullName = request.fullName
        )
    }
}