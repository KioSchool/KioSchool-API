package com.kioschool.kioschoolapi.domain.order.dto.common

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import java.time.LocalDateTime

data class OrderSessionWithOrderDto(
    val id: Long,
    val expectedEndAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val tableNumber: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val orders: List<OrderDto>
) {
    companion object {
        fun of(orderSession: OrderSession, orders: List<Order>): OrderSessionWithOrderDto {
            return OrderSessionWithOrderDto(
                id = orderSession.id,
                expectedEndAt = orderSession.expectedEndAt,
                endAt = orderSession.endAt,
                tableNumber = orderSession.tableNumber,
                createdAt = orderSession.createdAt,
                updatedAt = orderSession.updatedAt,
                orders = orders.map { OrderDto.of(it) }
            )
        }
    }
}
