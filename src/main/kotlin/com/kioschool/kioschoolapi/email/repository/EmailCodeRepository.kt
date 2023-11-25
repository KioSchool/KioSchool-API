package com.kioschool.kioschoolapi.email.repository

import com.kioschool.kioschoolapi.email.entity.EmailCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailCodeRepository : JpaRepository<EmailCode, Long> {
    fun findByEmail(email: String): EmailCode?

    fun deleteByEmail(email: String)
}
