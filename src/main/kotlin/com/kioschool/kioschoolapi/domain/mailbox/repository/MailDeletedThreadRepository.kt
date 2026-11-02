package com.kioschool.kioschoolapi.domain.mailbox.repository

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailDeletedThread
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MailDeletedThreadRepository : JpaRepository<MailDeletedThread, Long> {
    fun findByResendThreadId(resendThreadId: String): MailDeletedThread?
}
