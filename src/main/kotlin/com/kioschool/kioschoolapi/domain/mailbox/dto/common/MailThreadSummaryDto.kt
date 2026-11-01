package com.kioschool.kioschoolapi.domain.mailbox.dto.common

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import java.time.LocalDateTime

data class MailThreadSummaryDto(
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
) {
    companion object {
        fun of(thread: MailThread) = MailThreadSummaryDto(
            id = thread.id,
            subject = thread.subject,
            status = thread.status(),
            isUnread = thread.isUnread(),
            workspaceId = thread.workspace?.id,
            workspaceName = thread.workspace?.name,
            counterpartName = thread.counterpartName,
            counterpartEmail = thread.counterpartEmail,
            lastMessageAt = thread.lastMessageAt,
        )
    }
}
