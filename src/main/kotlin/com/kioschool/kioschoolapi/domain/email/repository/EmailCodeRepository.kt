package com.kioschool.kioschoolapi.domain.email.repository

import com.kioschool.kioschoolapi.domain.email.entity.EmailCode
import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

@Repository
interface EmailCodeRepository : JpaRepository<EmailCode, Long> {
    fun findByEmailAndKind(email: String, kind: EmailKind): EmailCode?

    // 동시에 들어온 인증 시도가 같은 시도 횟수를 읽고 각자 통과하지 않도록 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedByEmailAndKind(email: String, kind: EmailKind): EmailCode?

    fun deleteByEmailAndKind(email: String, kind: EmailKind)
    fun deleteAllByEmail(email: String)

    fun findByCodeAndKind(code: String, kind: EmailKind): EmailCode?
}
