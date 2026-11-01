package com.kioschool.kioschoolapi.domain.mailbox.repository

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MailMessageRepository : JpaRepository<MailMessage, Long> {
    fun findByResendEmailId(resendEmailId: String): MailMessage?
}
