package com.kioschool.kioschoolapi.domain.email.repository

import com.kioschool.kioschoolapi.domain.email.entity.EmailCode
import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailCodeRepository : JpaRepository<EmailCode, Long> {
    fun findByEmailAndKind(email: String, kind: EmailKind): EmailCode?

    fun deleteByEmailAndKind(email: String, kind: EmailKind)

    fun findByCodeAndKind(code: String, kind: EmailKind): EmailCode?
}
