package com.kioschool.kioschoolapi.domain.order.dto

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import java.time.LocalDateTime

data class OrderDto(
    val id: Long,
    val tableNumber: Int,
    val customerName: String,
    val orderProducts: List<OrderProductDto>,
    val totalPrice: Int,
    val status: OrderStatus,
    val orderNumber: Long,
    val orderSession: OrderSessionDto?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(order: Order): OrderDto {
            return OrderDto(
                id = order.id,
                tableNumber = order.tableNumber,
                customerName = order.customerName,
                orderProducts = order.orderProducts.map { OrderProductDto.of(it) },
                totalPrice = order.totalPrice,
                status = order.status,
                orderNumber = order.orderNumber,
                orderSession = order.orderSession?.let { OrderSessionDto.of(it) },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt
            )
        }
    }
}
