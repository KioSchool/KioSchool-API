package com.kioschool.kioschoolapi.domain.email.schedule

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 가입 인증 코드와 비밀번호 재설정 코드는 쓰지 않으면 이메일과 함께 남는다. 개인정보처리방침에 적은 기간이 지나면 지운다.
 * 재설정 링크도 이 기간이 지나면 더는 쓸 수 없다.
 */
@Component
class EmailCodePurgeScheduler(
    private val emailService: EmailService,
    @Value("\${email.code.retention-days}")
    private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun purgeOnStartup() {
        purgeExpiredCodes()
    }

    @Scheduled(cron = "0 55 4 * * *", zone = "Asia/Seoul")
    fun purgeExpiredCodes() {
        // 서버 시작 때도 불리므로 실패가 기동을 막지 않게 로그만 남긴다.
        runCatching { emailService.deleteCodesUpdatedBefore(LocalDateTime.now().minusDays(retentionDays)) }
            .onSuccess { if (it > 0) log.info("Expired email codes purged: {}", it) }
            .onFailure { log.error("Failed to purge email codes", it) }
    }
}
