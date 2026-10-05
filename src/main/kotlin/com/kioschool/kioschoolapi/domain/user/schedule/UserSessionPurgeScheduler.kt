package com.kioschool.kioschoolapi.domain.user.schedule

import com.kioschool.kioschoolapi.domain.user.service.UserSessionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 로그아웃하지 않고 떠난 기기의 세션은 만료 뒤에도 행으로 남는다. 하루 한 번 지운다.
 */
@Component
class UserSessionPurgeScheduler(
    private val userSessionService: UserSessionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
    fun purgeExpiredSessions() {
        val purged = userSessionService.purgeExpired()
        if (purged > 0) log.info("Expired user sessions purged: {}", purged)
    }
}
