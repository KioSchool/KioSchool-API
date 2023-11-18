package com.kioschool.kioschoolapi.user.repository

import com.kioschool.kioschoolapi.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByLoginId(loginId: String): User?
}