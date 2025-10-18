package com.kioschool.kioschoolapi.domain.order.dto

import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import java.time.LocalDateTime

data class OrderProductDto(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productPrice: Int,
    val quantity: Int,
    val servedCount: Int,
    val isServed: Boolean,
    val totalPrice: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(orderProduct: OrderProduct): OrderProductDto {
            return OrderProductDto(
                id = orderProduct.id,
                productId = orderProduct.productId,
                productName = orderProduct.productName,
                productPrice = orderProduct.productPrice,
                quantity = orderProduct.quantity,
                servedCount = orderProduct.servedCount,
                isServed = orderProduct.isServed,
                totalPrice = orderProduct.totalPrice,
                createdAt = orderProduct.createdAt,
                updatedAt = orderProduct.updatedAt
            )
        }
    }
}
