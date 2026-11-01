package com.kioschool.kioschoolapi.domain.mailbox.listener

import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailThreadWriter
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("taskExecutor")
    fun handle(event: ResendWebhookEvent) {
        runCatching {
            when (event.type) {
                EMAIL_RECEIVED -> event.threadId?.let { threadId ->
                    if (mailSyncService.syncThread(threadId).newInboundCount > 0) discordService.sendNewMail()
                }

                EMAIL_SENT -> event.threadId?.let { mailSyncService.syncThread(it) }
                EMAIL_BOUNCED -> event.emailId?.let { mailThreadWriter.markBounced(it, LocalDateTime.now()) }
                else -> Unit
            }
        }.onFailure {
            log.error("Resend webhook failed: type={}, threadId={}, emailId={}", event.type, event.threadId, event.emailId, it)
        }
    }

    companion object {
        private const val EMAIL_RECEIVED = "inbox.email.received"
        private const val EMAIL_SENT = "inbox.email.sent"
        private const val EMAIL_BOUNCED = "email.bounced"
    }
}
