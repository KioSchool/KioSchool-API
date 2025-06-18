package com.kioschool.kioschoolapi.domain.order.dto

data class CreateOrderRequestBody(
    val workspaceId: Long,
    val tableNumber: Int,
    val orderProducts: List<OrderProductRequestBody>,
    val customerName: String
)