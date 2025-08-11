package com.kioschool.kioschoolapi.global.common.schedule

import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class Scheduler(
    private val orderService: OrderService,
    private val orderSessionRepository: OrderSessionRepository
) {
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun resetAllOrderNumber() {
        orderService.resetAllOrderNumber()
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun resetAllOrderSession() {
        val notEndedOrderSessions = orderSessionRepository.findAllByEndAtIsNull()

        notEndedOrderSessions.forEach { orderSession ->
            orderSession.endAt = LocalDateTime.now()
            orderSessionRepository.save(orderSession)
        }
    }
}

