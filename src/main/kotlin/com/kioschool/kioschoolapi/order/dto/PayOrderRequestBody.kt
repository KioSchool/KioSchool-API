package com.kioschool.kioschoolapi.order.dto

data class PayOrderRequestBody(
    val workspaceId: Long,
    val orderId: Long
)