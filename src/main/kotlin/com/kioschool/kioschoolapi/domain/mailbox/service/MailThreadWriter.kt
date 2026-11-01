package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailAttachment
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailMessageRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.resend.dto.ResendThreadEmail
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
class MailThreadWriter(
    private val mailThreadRepository: MailThreadRepository,
    private val mailMessageRepository: MailMessageRepository,
    private val workspaceRepository: WorkspaceRepository,
    @Value("\${mail.retention-days}")
    private val retentionDays: Long,
) {
    /** 이벤트 종류를 가리지 않고 스레드 전체를 맞추므로 재시도·중복·순서 뒤바뀜에 안전하다. */
    @Transactional
    fun upsert(resendThreadId: String, emails: List<ResendThreadEmail>): MailSyncResult {
        if (emails.isEmpty()) return MailSyncResult(0)

        val sorted = emails.sortedBy { it.receivedAt }
        val latest = sorted.last()
        val existingMessages = sorted.associate { it.id to mailMessageRepository.findByResendEmailId(it.id) }
        val newInboundCount = sorted.count { it.isInbound() && existingMessages[it.id] == null }

        val counterpart = counterpartOf(sorted)
        val thread = mailThreadRepository.findByResendThreadId(resendThreadId)
            ?: MailThread(
                resendThreadId = resendThreadId,
                subject = null,
                workspace = null,
                counterpartEmail = counterpart.email,
                counterpartName = counterpart.name,
                lastMessageAt = latest.receivedAt.toKst(),
                lastDirection = latest.direction(),
                purgeAt = latest.receivedAt.toKst(),
            )
        thread.subject = sorted.firstNotNullOfOrNull { it.subject?.withoutNul() } ?: thread.subject
        thread.counterpartEmail = counterpart.email
        thread.counterpartName = counterpart.name
        thread.lastMessageAt = latest.receivedAt.toKst()
        thread.lastDirection = latest.direction()
        thread.hasInbound = thread.hasInbound || sorted.any { it.isInbound() }
        if (newInboundCount > 0) thread.resolvedAt = null

        val hint = existingMessages.values.firstNotNullOfOrNull { it?.workspaceHint }
        if (thread.workspace == null) {
            thread.workspace = hint
                ?: workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdDesc(counterpart.email)
        }
        if (existingMessages.values.any { it?.campaignRecipientId != null }) thread.fromCampaign = true
        thread.renewPurgeAt(retentionDays)
        val savedThread = mailThreadRepository.save(thread)

        sorted.forEach { email ->
            val message = existingMessages[email.id] ?: MailMessage(resendEmailId = email.id)
            fill(message, email, savedThread)
            mailMessageRepository.save(message)
        }

        return MailSyncResult(newInboundCount)
    }

    /** 응답의 이메일 ID로 보낸 사람과 주점을 남긴다. 웹훅이 먼저 왔으면 같은 행에 더한다. */
    @Transactional
    fun recordOutbound(
        emailId: String,
        sentBy: User?,
        workspace: Workspace?,
        campaignRecipientId: Long?,
        thread: MailThread?,
        subject: String,
        text: String,
        html: String,
        to: String,
    ) {
        val now = LocalDateTime.now()
        val message = mailMessageRepository.findByResendEmailId(emailId)
            ?: MailMessage(
                resendEmailId = emailId,
                direction = MailDirection.OUTBOUND,
                toEmail = to,
                subject = subject,
                textBody = text,
                htmlBody = html,
                sentAt = now,
            )
        message.sentBy = sentBy
        message.workspaceHint = workspace
        message.campaignRecipientId = campaignRecipientId

        // 답장은 스레드를 이미 알고 있으므로 웹훅을 기다리지 않고 바로 답장 대기로 돌린다
        val replied = thread?.let { mailThreadRepository.findByIdOrNull(it.id) ?: it }
        if (replied != null) {
            message.thread = message.thread ?: replied
            replied.lastDirection = MailDirection.OUTBOUND
            replied.lastMessageAt = now
            replied.resolvedAt = null
            replied.renewPurgeAt(retentionDays)
        }

        message.thread?.let { linked ->
            if (linked.workspace == null) linked.workspace = workspace
            if (campaignRecipientId != null) linked.fromCampaign = true
        }
        mailMessageRepository.save(message)
    }

    private fun fill(message: MailMessage, email: ResendThreadEmail, thread: MailThread) {
        val from = MailAddressParser.parse(email.from)
        message.thread = thread
        message.direction = email.direction()
        message.fromEmail = from.email
        message.fromName = from.name?.withoutNul()
        message.toEmail = email.to.firstOrNull()?.let { MailAddressParser.parse(it).email }
        message.subject = email.subject?.withoutNul()
        message.textBody = email.text?.withoutNul()
        message.htmlBody = email.html?.withoutNul()
        message.sentAt = email.receivedAt.toKst()

        val saved = message.attachments.map { it.resendAttachmentId }.toSet()
        email.attachments.filter { it.id !in saved }.forEach {
            message.attachments += MailAttachment(message, it.id, it.filename?.withoutNul(), it.size)
        }
    }

    private fun counterpartOf(sorted: List<ResendThreadEmail>): ParsedMailAddress {
        val lastInbound = sorted.lastOrNull { it.isInbound() }
        if (lastInbound != null) return MailAddressParser.parse(lastInbound.from).clean()
        val recipient = sorted.firstNotNullOfOrNull { it.to.firstOrNull() } ?: sorted.first().from
        return MailAddressParser.parse(recipient).clean()
    }

    private fun ParsedMailAddress.clean() = copy(name = name?.withoutNul())

    private fun ResendThreadEmail.isInbound() = direction == INBOUND

    private fun ResendThreadEmail.direction() = if (isInbound()) MailDirection.INBOUND else MailDirection.OUTBOUND

    private fun OffsetDateTime.toKst() = atZoneSameInstant(KST).toLocalDateTime()

    // PostgreSQL TEXT는 NUL 문자를 저장하지 못한다
    private fun String.withoutNul() = replace("\u0000", "")

    companion object {
        private const val INBOUND = "inbound"
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
