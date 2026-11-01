package com.kioschool.kioschoolapi.domain.mailbox.listener

import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailThreadWriter
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.Executor

data class ResendWebhookEvent(
    val type: String,
    val inboxId: String?,
    val threadId: String?,
    val emailId: String?,
)

@Component
class ResendWebhookHandler(
    private val mailSyncService: MailSyncService,
    private val mailThreadWriter: MailThreadWriter,
    private val discordService: DiscordService,
    private val mailRetryScheduler: MailRetryScheduler,
    @Qualifier("taskExecutor")
    private val taskExecutor: Executor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("taskExecutor")
    fun handle(event: ResendWebhookEvent) {
        process(event, attempt = 0)
    }

    // 웹훅에 이미 200을 줬으므로 Resend가 다시 보내지 않는다. 여기서 놓치면 메일을 잃으니 직접 다시 시도한다.
    private fun process(event: ResendWebhookEvent, attempt: Int) {
        runCatching { apply(event) }.onFailure { error ->
            log.error("Resend webhook failed: type={}, threadId={}, emailId={}, attempt={}", event.type, event.threadId, event.emailId, attempt, error)

            val delay = RETRY_DELAYS.getOrNull(attempt)
            if (delay == null) {
                runCatching { discordService.sendMailSyncFailed() }
                    .onFailure { log.error("Failed to send mail sync alert", it) }
                return
            }
            mailRetryScheduler.schedule(
                { taskExecutor.execute { process(event, attempt + 1) } },
                Instant.now().plus(delay),
            )
        }
    }

    private fun apply(event: ResendWebhookEvent) {
        when (event.type) {
            EMAIL_RECEIVED -> event.threadId?.let { threadId ->
                if (mailSyncService.syncThread(threadId).newInboundCount > 0) notifyNewMail()
            }

            EMAIL_SENT -> event.threadId?.let { mailSyncService.syncThread(it) }
            EMAIL_BOUNCED -> event.emailId?.let { mailThreadWriter.markBounced(it, LocalDateTime.now()) }
        }
    }

    private fun notifyNewMail() {
        runCatching { discordService.sendNewMail() }
            .onFailure { log.error("Failed to send new mail alert", it) }
    }

    companion object {
        private const val EMAIL_RECEIVED = "inbox.email.received"
        private const val EMAIL_SENT = "inbox.email.sent"
        private const val EMAIL_BOUNCED = "email.bounced"
        private val RETRY_DELAYS = listOf(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30))
    }
}
