package com.lc9th5.myfamily.config

import com.lc9th5.myfamily.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val userRepository: UserRepository
) {

    // Mã hóa mật khẩu 
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12) 
    
    // Cung cấp UserDetailsService để load user từ database
    @Bean
    fun userDetailsService(): UserDetailsService = UserDetailsService { username ->
        val user = if (username.contains("@")) {
            userRepository.findByEmail(username)
        } else {
            userRepository.findByUsername(username)
        } ?: throw org.springframework.security.core.userdetails.UsernameNotFoundException("User not found")
        org.springframework.security.core.userdetails.User
            .withUsername(user.email)
            .password(user.password)
            .authorities(user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") })
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationManager {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return ProviderManager(provider)
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF cho API
            .cors(withDefaults()) // Enable CORS với cấu hình mặc định
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // Stateless session (JWT)
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**", "/api/users/register").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->// Cấu hình Resource Server để sử dụng JWT
                oauth2.jwt(withDefaults())
            }
        return http.build()
    }
}