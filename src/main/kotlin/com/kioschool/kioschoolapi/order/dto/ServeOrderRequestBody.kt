package com.kioschool.kioschoolapi.order.dto

data class ServeOrderRequestBody(
    val workspaceId: Long,
    val orderId: Long
)