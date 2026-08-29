package com.kioschool.kioschoolapi.domain.inquiry.schedule

import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryPurgeService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class InquiryPurgeScheduler(
    private val inquiryRepository: InquiryRepository,
    private val inquiryPurgeService: InquiryPurgeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    fun purgeExpiredInquiries() {
        val expired = inquiryRepository.findAllByPurgeAtBefore(
            LocalDateTime.now(),
            PageRequest.of(0, BATCH_SIZE)
        )
        if (expired.isEmpty()) return

        var purged = 0
        var failed = 0

        expired.forEach { inquiry ->
            runCatching { inquiryPurgeService.purgeOne(inquiry.id) }
                .onSuccess { purged++ }
                .onFailure {
                    failed++
                    log.error("Failed to purge inquiryId={}", inquiry.id, it)
                }
        }

        log.info("Inquiry purge finished: purged={}, failed={}", purged, failed)
    }

    companion object {
        const val BATCH_SIZE = 100
    }
}
