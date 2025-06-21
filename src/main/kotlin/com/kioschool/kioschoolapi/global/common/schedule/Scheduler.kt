package com.kioschool.kioschoolapi.global.common.schedule

import com.kioschool.kioschoolapi.domain.order.service.OrderService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Scheduler(
    private val orderService: OrderService
) {
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun resetAllOrderNumber() {
        orderService.resetAllOrderNumber()
    }
}

