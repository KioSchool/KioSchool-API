package com.kioschool.kioschoolapi.domain.order.dto

data class OrderProductRequestBody(
    val productId: Long,
    val quantity: Int,
)