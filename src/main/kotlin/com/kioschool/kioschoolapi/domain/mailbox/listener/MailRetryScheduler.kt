package com.kioschool.kioschoolapi.domain.mailbox.listener

import jakarta.annotation.PreDestroy
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import java.time.Instant

// TaskScheduler를 빈으로 등록하면 @Scheduled가 쓸 스케줄러 선택이 바뀌므로 내부에 둔다
@Component
class MailRetryScheduler {
    private val scheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("mail-retry-")
        initialize()
    }

    fun schedule(task: Runnable, at: Instant) {
        scheduler.schedule(task, at)
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdown()
    }
}
