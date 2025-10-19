package com.kioschool.kioschoolapi.domain.order.dto.request

data class ServeOrderRequestBody(
    val workspaceId: Long,
    val orderId: Long
)