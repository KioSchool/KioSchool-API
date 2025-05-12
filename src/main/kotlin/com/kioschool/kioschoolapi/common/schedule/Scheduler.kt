package com.kioschool.kioschoolapi.common.schedule

import com.kioschool.kioschoolapi.order.service.OrderService
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

