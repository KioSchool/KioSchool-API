package com.kioschool.kioschoolapi.domain.order.dto.common

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.PaymentMethod
import java.time.LocalDateTime

import com.kioschool.kioschoolapi.domain.order.dto.common.OrderProductDto
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderSessionDto

data class OrderDto(
    val id: Long,
    val tableNumber: Int,
    val customerName: String,
    val orderProducts: List<OrderProductDto>,
    val totalPrice: Int,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod?,
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
                paymentMethod = order.paymentMethod,
                orderNumber = order.orderNumber,
                orderSession = order.orderSession?.let { OrderSessionDto.of(it) },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt
            )
        }
    }
}
