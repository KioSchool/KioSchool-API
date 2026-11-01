package com.kioschool.kioschoolapi.domain.mailbox.schedule

import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailPurgeService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MailPurgeScheduler(
    private val mailThreadRepository: MailThreadRepository,
    private val mailPurgeService: MailPurgeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
    fun purgeExpired() {
        val expired = mailThreadRepository.findAllByPurgeAtBefore(
            LocalDateTime.now(),
            PageRequest.of(0, BATCH_SIZE, Sort.by("purgeAt"))
        )
        if (expired.isEmpty()) return

        var purged = 0
        var failed = 0

        expired.forEach { thread ->
            runCatching { mailPurgeService.purgeOne(thread.id) }
                .onSuccess { purged++ }
                .onFailure {
                    failed++
                    log.error("Failed to purge mailThreadId={}", thread.id, it)
                }
        }

        log.info("Mail purge finished: purged={}, failed={}", purged, failed)
    }

    companion object {
        const val BATCH_SIZE = 100
    }
}
