package com.kioschool.kioschoolapi.domain.order.dto.common

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.PaymentMethod
import java.time.LocalDateTime

data class SuperAdminOrderDto(
    val id: Long,
    val workspaceId: Long,
    val workspaceName: String,
    val tableNumber: Int,
    val customerName: String,
    val orderProducts: List<OrderProductDto>,
    val totalPrice: Int,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod?,
    val orderNumber: Long,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(order: Order): SuperAdminOrderDto {
            return SuperAdminOrderDto(
                id = order.id,
                workspaceId = order.workspace.id,
                workspaceName = order.workspace.name,
                tableNumber = order.tableNumber,
                customerName = order.customerName,
                orderProducts = order.orderProducts.map { OrderProductDto.of(it) },
                totalPrice = order.totalPrice,
                status = order.status,
                paymentMethod = order.paymentMethod,
                orderNumber = order.orderNumber,
                createdAt = order.createdAt,
                updatedAt = order.updatedAt
            )
        }
    }
}
