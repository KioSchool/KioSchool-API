package com.kioschool.kioschoolapi.domain.order.schedule

import com.kioschool.kioschoolapi.domain.order.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 손님 입금자명은 입금 확인과 축제 뒤 결산에만 쓴다. 개인정보처리방침에 적은 보관 기간이 지나면 가린다.
 *
 * 서버가 뜰 때도 한 번 돈다. 백업에서 복원하면 가렸던 이름이 돌아오는데, 다음 새벽까지 그대로 보이지 않게 하려는 것이다.
 */
@Component
class CustomerNameMaskScheduler(
    private val orderService: OrderService,
    @Value("\${order.customer-name.retention-days}")
    private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun maskOnStartup() {
        maskExpiredCustomerNames()
    }

    @Scheduled(cron = "0 50 4 * * *", zone = "Asia/Seoul")
    fun maskExpiredCustomerNames() {
        // 서버 시작 때도 불리므로 실패가 기동을 막지 않게 로그만 남긴다.
        runCatching { orderService.maskCustomerNamesCreatedBefore(LocalDateTime.now().minusDays(retentionDays)) }
            .onSuccess {
                if (it.total > 0) log.info("Customer names masked: orders={}, orderSessions={}", it.orders, it.orderSessions)
            }
            .onFailure { log.error("Failed to mask customer names", it) }
    }
}
