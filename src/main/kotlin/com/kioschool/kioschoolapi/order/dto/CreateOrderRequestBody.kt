package com.kioschool.kioschoolapi.order.dto

data class CreateOrderRequestBody(
    val workspaceId: Long,
    val tableNumber: Int,
    val orderProducts: List<OrderProductRequestBody>,
    val customerName: String
)