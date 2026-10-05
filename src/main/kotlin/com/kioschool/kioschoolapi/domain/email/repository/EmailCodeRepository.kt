package com.kioschool.kioschoolapi.domain.email.repository

import com.kioschool.kioschoolapi.domain.email.entity.EmailCode
import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EmailCodeRepository : JpaRepository<EmailCode, Long> {
    fun findByEmailAndKind(email: String, kind: EmailKind): EmailCode?

    fun deleteByEmailAndKind(email: String, kind: EmailKind)

    fun findByCodeAndKind(code: String, kind: EmailKind): EmailCode?

    @Modifying
    @Query("DELETE FROM EmailCode e WHERE e.updatedAt < :cutoff")
    fun deleteAllUpdatedBefore(@Param("cutoff") cutoff: LocalDateTime): Int
}
