package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.global.resend.service.ResendDailyLimitException
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class MailCampaignSender(
    private val mailCampaignQueue: MailCampaignQueue,
    private val mailCampaignComposer: MailCampaignComposer,
    private val resendInboxClient: ResendInboxClient,
    private val mailSyncService: MailSyncService,
    @Value("\${mail.campaign.daily-limit}")
    private val dailyLimit: Int,
    @Value("\${mail.campaign.send-interval-millis:1000}")
    private val sendIntervalMillis: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 시작 요청과 스케줄러가 겹쳐도 한도를 넘지 않도록 한 번에 하나만 돌린다
    private val sendLock = ReentrantLock()

    // 스케줄러가 10분마다 도니, 일일 한도를 다 쓴 날은 다시 호출하지 않는다
    @Volatile
    private var quotaExhaustedOn: LocalDate? = null

    @Async("taskExecutor")
    fun sendDueAsync() {
        runCatching { sendDue() }.onFailure { log.error("Mail campaign sending failed", it) }
    }

    fun sendDue(now: LocalDateTime = LocalDateTime.now(KST)) {
        if (!isSendingHour(now) || quotaExhaustedOn == now.toLocalDate()) return

        sendLock.withLock {
            mailCampaignQueue.releaseStale(now.minusMinutes(STALE_MINUTES))

            var remaining = dailyLimit - todayAttempts(now)
            while (remaining > 0) {
                val claim = mailCampaignQueue.claimNext(now) ?: break
                if (claim !is MailCampaignClaim.Send) continue
                if (!sendOne(claim, now)) break

                remaining--
                if (sendIntervalMillis > 0) Thread.sleep(sendIntervalMillis)
            }

            mailCampaignQueue.finishCompleted(now)
        }
    }

    fun todayAttempts(now: LocalDateTime): Long = mailCampaignQueue.todayAttempts(now)

    /** 오늘 더 보낼 수 없으면 false를 돌려준다. */
    private fun sendOne(claim: MailCampaignClaim.Send, now: LocalDateTime): Boolean {
        val workspace = claim.workspace
        val values = MailTemplateValues(workspace.name, claim.ownerName, workspace.id, claim.stats)
        val mail = mailCampaignComposer.compose(claim.content, values, claim.unsubscribeToken)

        val emailId = try {
            resendInboxClient.sendNew(
                claim.email,
                mail.subject,
                mail.html,
                mail.text,
                mail.headers,
                "mail-campaign-${claim.recipientId}-${claim.attempt}",
            )
        } catch (e: ResendDailyLimitException) {
            log.warn("Mail campaign paused by Resend limit: recipientId={}", claim.recipientId)
            mailCampaignQueue.returnToPending(claim.recipientId)
            quotaExhaustedOn = now.toLocalDate()
            return false
        } catch (e: ResendSendException) {
            log.error("Mail campaign send failed: recipientId={}, reason={}", claim.recipientId, e.message)
            mailCampaignQueue.markFailed(claim.recipientId, FAILURE_REASON)
            return true
        }

        mailCampaignQueue.markSent(claim.recipientId, emailId, now)
        runCatching {
            mailSyncService.recordOutbound(
                emailId, claim.createdBy, workspace, claim.recipientId, null, mail.subject, mail.text, mail.html, claim.email,
            )
        }.onFailure { log.error("Failed to record campaign mail: recipientId={}", claim.recipientId, it) }
        return true
    }

    private fun isSendingHour(now: LocalDateTime): Boolean {
        val time = now.toLocalTime()
        return !time.isBefore(SENDING_START) && time.isBefore(SENDING_END)
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
        private val SENDING_START = LocalTime.of(10, 0)
        private val SENDING_END = LocalTime.of(20, 0)
        private const val STALE_MINUTES = 10L
        private const val FAILURE_REASON = "보내지 못했어요"
    }
}
