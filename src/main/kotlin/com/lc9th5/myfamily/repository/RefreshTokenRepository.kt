package com.lc9th5.myfamily.repository

import com.lc9th5.myfamily.model.user.RefreshToken
import com.lc9th5.myfamily.model.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun findByUserAndExpiresAtAfter(user: User, expiresAt: LocalDateTime): RefreshToken?
    fun deleteByUser(user: User)
}