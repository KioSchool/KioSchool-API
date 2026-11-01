package com.kioschool.kioschoolapi.domain.mailbox.entity

import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

// 보내는 흐름과 웹훅 중 어느 쪽이 먼저 와도 되도록, 스레드와 본문을 아직 모르는 행도 저장한다
@Entity
@Table(name = "mail_message")
class MailMessage(
    var resendEmailId: String,
    @ManyToOne(fetch = FetchType.LAZY)
    var thread: MailThread? = null,
    @Enumerated(EnumType.STRING)
    var direction: MailDirection? = null,
    var fromEmail: String? = null,
    @Column(columnDefinition = "TEXT")
    var fromName: String? = null,
    var toEmail: String? = null,
    @Column(columnDefinition = "TEXT")
    var subject: String? = null,
    @Column(columnDefinition = "TEXT")
    var textBody: String? = null,
    @Column(columnDefinition = "TEXT")
    var htmlBody: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_user_id")
    var sentBy: User? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    var workspaceHint: Workspace? = null,
    var campaignRecipientId: Long? = null,
    var sentAt: LocalDateTime? = null,
    var bouncedAt: LocalDateTime? = null,
    @OneToMany(mappedBy = "message", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id")
    val attachments: MutableList<MailAttachment> = mutableListOf(),
) : BaseEntity()
