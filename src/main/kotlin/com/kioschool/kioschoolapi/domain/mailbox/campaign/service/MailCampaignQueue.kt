package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRecipientRepository
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailUnsubscribeService
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

sealed interface MailCampaignClaim {
    data object Excluded : MailCampaignClaim

    data class Send(
        val recipientId: Long,
        val attempt: Int,
        val email: String,
        val workspace: Workspace,
        val ownerName: String,
        val stats: MailAudienceStats,
        val createdBy: User?,
        val content: MailCampaignContent,
        val unsubscribeToken: String,
    ) : MailCampaignClaim
}

// Resend 호출을 트랜잭션 밖에 두려고 수신자 상태 변경을 짧은 트랜잭션으로 나눠 둔다
@Service
class MailCampaignQueue(
    private val mailCampaignRepository: MailCampaignRepository,
    private val mailCampaignRecipientRepository: MailCampaignRecipientRepository,
    private val mailUnsubscribeService: MailUnsubscribeService,
    private val objectMapper: ObjectMapper,
    @Value("\${mail.retention-days}")
    private val retentionDays: Long,
) {
    /** 대기 중에 탈퇴·수신 거부·주소 변경이 있을 수 있어, 보낼 차례에 사장 정보를 다시 읽는다. */
    @Transactional
    fun claimNext(now: LocalDateTime): MailCampaignClaim? {
        val recipient = mailCampaignRecipientRepository.findNextPendingForUpdate() ?: return null
        recipient.claimedAt = now

        val workspace = recipient.workspace
        val owner = workspace?.owner
        val email = owner?.email
        val excludedReason = when {
            workspace == null || owner == null -> "주점이 삭제됐어요"
            owner.withdrawnAt != null -> "탈퇴한 회원이에요"
            email.isNullOrBlank() -> "이메일이 없어요"
            owner.mailUnsubscribedAt != null -> "수신 거부한 분이에요"
            else -> null
        }
        if (excludedReason != null) {
            recipient.status = MailRecipientStatus.EXCLUDED
            recipient.failureReason = excludedReason
            mailCampaignRecipientRepository.save(recipient)
            return MailCampaignClaim.Excluded
        }

        recipient.status = MailRecipientStatus.SENDING
        recipient.email = email!!
        recipient.name = owner!!.name
        recipient.workspaceName = workspace!!.name
        mailCampaignRecipientRepository.save(recipient)

        val campaign = recipient.campaign
        return MailCampaignClaim.Send(
            recipientId = recipient.id,
            attempt = recipient.attempt,
            email = email,
            workspace = workspace,
            ownerName = owner.name,
            stats = recipient.statsJson?.let { objectMapper.readValue(it, MailAudienceStats::class.java) } ?: MailAudienceStats(),
            createdBy = campaign.createdBy,
            content = MailCampaignContent(campaign.subject, campaign.bodyTemplate),
            unsubscribeToken = mailUnsubscribeService.tokenFor(owner),
        )
    }

    @Transactional
    fun markSent(recipientId: Long, emailId: String, at: LocalDateTime) = changeRecipient(recipientId) {
        it.status = MailRecipientStatus.SENT
        it.resendEmailId = emailId
        it.sentAt = at
        it.failureReason = null
    }

    @Transactional
    fun markFailed(recipientId: Long, reason: String) = changeRecipient(recipientId) {
        it.status = MailRecipientStatus.FAILED
        it.failureReason = reason
    }

    /** 일일 한도 초과는 실패가 아니므로 시도 수에서 빼고 다음 날로 넘긴다. */
    @Transactional
    fun returnToPending(recipientId: Long) = changeRecipient(recipientId) {
        it.status = MailRecipientStatus.PENDING
        it.claimedAt = null
    }

    /** 서버가 보내는 도중 내려간 행을 되돌린다. attempt가 그대로라 같은 Idempotency-Key로 다시 보낸다. */
    @Transactional
    fun releaseStale(before: LocalDateTime) {
        mailCampaignRecipientRepository.findAllByStatusAndClaimedAtBefore(MailRecipientStatus.SENDING, before).forEach {
            it.status = MailRecipientStatus.PENDING
            it.claimedAt = null
            mailCampaignRecipientRepository.save(it)
        }
    }

    @Transactional(readOnly = true)
    fun todayAttempts(now: LocalDateTime): Long {
        val start = now.toLocalDate().atStartOfDay()
        return mailCampaignRecipientRepository.countAttempts(start, start.plusDays(1), ATTEMPTED_STATUSES)
    }

    @Transactional
    fun finishCompleted(now: LocalDateTime) {
        mailCampaignRepository.findAllByStatus(MailCampaignStatus.SENDING)
            .filter { mailCampaignRecipientRepository.countUnfinished(it) == 0L }
            .forEach {
                it.status = MailCampaignStatus.DONE
                it.purgeAt = now.plusDays(retentionDays)
                mailCampaignRepository.save(it)
            }
    }

    @Transactional
    fun markBounced(emailId: String) {
        val recipient = mailCampaignRecipientRepository.findByResendEmailId(emailId) ?: return
        recipient.status = MailRecipientStatus.FAILED
        recipient.failureReason = BOUNCED_REASON
        mailCampaignRecipientRepository.save(recipient)
    }

    private fun changeRecipient(recipientId: Long, change: (MailCampaignRecipient) -> Unit) {
        val recipient = mailCampaignRecipientRepository.findByIdOrNull(recipientId) ?: return
        change(recipient)
        mailCampaignRecipientRepository.save(recipient)
    }

    companion object {
        private const val BOUNCED_REASON = "메일 주소를 찾을 수 없어요"

        // 처음 대기 중인 행과 한도 초과로 되돌린 행은 claimedAt이 비어 있어 세지 않는다.
        // PENDING은 실패를 다시 보내려고 되돌린 행이다. 재시도로 하루 한도를 넘지 않도록 센다.
        private val ATTEMPTED_STATUSES = setOf(
            MailRecipientStatus.SENT,
            MailRecipientStatus.FAILED,
            MailRecipientStatus.SENDING,
            MailRecipientStatus.PENDING,
        )
    }
}
