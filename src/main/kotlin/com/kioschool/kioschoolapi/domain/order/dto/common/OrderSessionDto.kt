package com.kioschool.kioschoolapi.domain.order.dto.common

import com.kioschool.kioschoolapi.domain.order.entity.GhostType
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import java.time.LocalDateTime

data class OrderSessionDto(
    val id: Long,
    val expectedEndAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val tableNumber: Int,
    val usageTime: Int,
    val totalOrderPrice: Long,
    val orderCount: Int,
    val ghostType: GhostType,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(orderSession: OrderSession): OrderSessionDto {
            return OrderSessionDto(
                id = orderSession.id,
                expectedEndAt = orderSession.expectedEndAt,
                endAt = orderSession.endAt,
                tableNumber = orderSession.tableNumber,
                usageTime = orderSession.usageTime,
                totalOrderPrice = orderSession.totalOrderPrice,
                orderCount = orderSession.orderCount,
                ghostType = orderSession.ghostType,
                createdAt = orderSession.createdAt,
                updatedAt = orderSession.updatedAt
            )
        }
    }
}
