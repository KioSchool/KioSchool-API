package com.kioschool.kioschoolapi.domain.mailbox.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "mail_attachment")
class MailAttachment(
    @ManyToOne(fetch = FetchType.LAZY)
    var message: MailMessage,
    var resendAttachmentId: String,
    @Column(columnDefinition = "TEXT")
    var filename: String?,
    var size: Long?,
) : BaseEntity()
