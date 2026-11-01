package com.kioschool.kioschoolapi.domain.mailbox.repository

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MailThreadRepository : JpaRepository<MailThread, Long> {
    fun findByResendThreadId(resendThreadId: String): MailThread?

    fun findAllByPurgeAtBefore(purgeAt: LocalDateTime, pageable: Pageable): List<MailThread>
}
