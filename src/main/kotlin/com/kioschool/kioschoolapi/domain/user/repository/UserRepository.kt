package com.kioschool.kioschoolapi.domain.user.repository

import com.kioschool.kioschoolapi.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByLoginId(loginId: String): User?

    fun findByEmail(email: String): User?
    fun findByNameContains(name: String, pageable: Pageable): Page<User>

    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.account IS NOT NULL")
    fun countUsersWithAccount(): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.account IS NULL")
    fun countUsersWithoutAccount(): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.account.tossAccountUrl IS NOT NULL")
    fun countUsersWithTossAccount(): Long
}