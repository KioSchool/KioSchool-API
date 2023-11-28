package com.kioschool.kioschoolapi.order.dto

data class OrderProductRequestBody(
    val productId: Long,
    val quantity: Int,
)