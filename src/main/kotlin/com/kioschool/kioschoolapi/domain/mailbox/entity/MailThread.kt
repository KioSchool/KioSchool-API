package com.kioschool.kioschoolapi.domain.mailbox.entity

import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "mail_thread")
class MailThread(
    var resendThreadId: String,
    @Column(columnDefinition = "TEXT")
    var subject: String?,
    @ManyToOne(fetch = FetchType.LAZY)
    var workspace: Workspace?,
    var counterpartEmail: String,
    @Column(columnDefinition = "TEXT")
    var counterpartName: String?,
    var lastMessageAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var lastDirection: MailDirection,
    var purgeAt: LocalDateTime,
    var lastReadAt: LocalDateTime? = null,
    var resolvedAt: LocalDateTime? = null,
    var spamAt: LocalDateTime? = null,
    var hasInbound: Boolean = false,
    var fromCampaign: Boolean = false,
) : BaseEntity() {
    fun status(): MailThreadStatus = when {
        spamAt != null -> MailThreadStatus.SPAM
        resolvedAt != null -> MailThreadStatus.RESOLVED
        lastDirection == MailDirection.INBOUND -> MailThreadStatus.NEEDS_REPLY
        else -> MailThreadStatus.WAITING
    }

    fun isUnread(): Boolean = lastReadAt?.isBefore(lastMessageAt) ?: true

    fun renewPurgeAt(retentionDays: Long) {
        val base = resolvedAt?.takeIf { it.isAfter(lastMessageAt) } ?: lastMessageAt
        purgeAt = base.plusDays(retentionDays)
    }
}
