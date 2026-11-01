package com.kioschool.kioschoolapi.domain.mailbox.dto.common

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailAttachment
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import java.time.LocalDateTime

data class MailThreadDetailDto(
    val id: Long,
    @field:LogMasked
    val subject: String?,
    val status: MailThreadStatus,
    val isUnread: Boolean,
    val workspaceId: Long?,
    val workspaceName: String?,
    @field:LogMasked
    val counterpartName: String?,
    val counterpartEmail: String,
    val lastMessageAt: LocalDateTime,
    val schoolName: String?,
    val messages: List<MailMessageDto>,
) {
    companion object {
        fun of(thread: MailThread, schoolName: String?, messages: List<MailMessage>) = MailThreadDetailDto(
            id = thread.id,
            subject = thread.subject,
            status = thread.status(),
            isUnread = thread.isUnread(),
            workspaceId = thread.workspace?.id,
            workspaceName = thread.workspace?.name,
            counterpartName = thread.counterpartName,
            counterpartEmail = thread.counterpartEmail,
            lastMessageAt = thread.lastMessageAt,
            schoolName = schoolName,
            messages = messages.map { MailMessageDto.of(it) },
        )
    }
}

data class MailMessageDto(
    val id: Long,
    val direction: MailDirection?,
    @field:LogMasked
    val fromName: String?,
    val fromEmail: String?,
    @field:LogMasked
    val sentByName: String?,
    @field:LogMasked
    val textBody: String?,
    @field:LogMasked
    val htmlBody: String?,
    val sentAt: LocalDateTime?,
    val bounced: Boolean,
    val attachments: List<MailAttachmentDto>,
) {
    companion object {
        fun of(message: MailMessage) = MailMessageDto(
            id = message.id,
            direction = message.direction,
            fromName = message.fromName,
            fromEmail = message.fromEmail,
            sentByName = message.sentBy?.name,
            textBody = message.textBody,
            htmlBody = message.htmlBody,
            sentAt = message.sentAt,
            bounced = message.bouncedAt != null,
            attachments = message.attachments.map { MailAttachmentDto.of(it) },
        )
    }
}

data class MailAttachmentDto(
    val id: Long,
    @field:LogMasked
    val filename: String?,
    val size: Long?,
) {
    companion object {
        fun of(attachment: MailAttachment) = MailAttachmentDto(attachment.id, attachment.filename, attachment.size)
    }
}
