package com.kioschool.kioschoolapi.domain.mailbox.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "mail_deleted_thread")
class MailDeletedThread(
    var resendThreadId: String,
    var deletedAt: LocalDateTime,
) : BaseEntity()
