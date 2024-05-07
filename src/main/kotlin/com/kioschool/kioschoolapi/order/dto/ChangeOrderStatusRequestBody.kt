package com.kioschool.kioschoolapi.order.dto

data class ChangeOrderStatusRequestBody(
    val workspaceId: Long,
    val orderId: Long,
    val status: String
)
