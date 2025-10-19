package com.kioschool.kioschoolapi.domain.order.dto.request

data class ChangeOrderStatusRequestBody(
    val workspaceId: Long,
    val orderId: Long,
    val status: String
)
