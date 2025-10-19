package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.domain.order.dto.request.OrderProductRequestBody

data class CreateOrderRequestBody(
    val workspaceId: Long,
    val tableNumber: Int,
    val orderProducts: List<OrderProductRequestBody>,
    val customerName: String
)