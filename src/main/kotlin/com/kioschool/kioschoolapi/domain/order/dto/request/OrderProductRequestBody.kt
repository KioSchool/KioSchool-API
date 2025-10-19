package com.kioschool.kioschoolapi.domain.order.dto.request

data class OrderProductRequestBody(
    val productId: Long,
    val quantity: Int,
)