package com.lc9th5.myfamily.security


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@Configuration
class UserDetailsConfig(private val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder) {

    @Bean
    fun userDetailsService(): UserDetailsService {
        val demoUser = User.withUsername("user@example.com")
            .password(passwordEncoder.encode("secret123"))
            .roles("USER")
            .build()
        return InMemoryUserDetailsManager(demoUser)
    }
}