package com.kioschool.kioschoolapi.domain.order.dto.common

import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import com.kioschool.kioschoolapi.domain.order.entity.OrderProductComponent
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
    val updatedAt: LocalDateTime?,
    // 세트만. 세트 1개당 할인 금액. 배포 전에 캐시된 주문에는 아래 두 필드가 없어서 기본값을 둔다.
    val discountPrice: Int? = null,
    // 세트 구성품. 일반 상품은 비어 있어서 세트인지는 이 목록으로 구분한다.
    val components: List<OrderComponentDto> = emptyList()
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
                updatedAt = orderProduct.updatedAt,
                discountPrice = orderProduct.discountPrice,
                components = orderProduct.components.map { OrderComponentDto.of(it) }
            )
        }
    }
}

data class OrderComponentDto(
    val id: Long,
    val name: String,
    // 세트 1개당 수량
    val quantity: Int,
    // quantity × 주문한 세트 수
    val totalQuantity: Int,
    val servedCount: Int,
    val displayOrder: Int
) {
    companion object {
        fun of(component: OrderProductComponent) = OrderComponentDto(
            id = component.id,
            name = component.name,
            quantity = component.quantity,
            totalQuantity = component.totalQuantity(),
            servedCount = component.servedCount,
            displayOrder = component.displayOrder
        )
    }
}
