package com.kioschool.kioschoolapi.domain.order.dto

data class ServeOrderRequestBody(
    val workspaceId: Long,
    val orderId: Long
)