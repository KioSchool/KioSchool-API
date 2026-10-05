package com.kioschool.kioschoolapi.domain.order.event

data class OrderProductServedCountChangedEvent(
    val workspaceId: Long,
    val orderProductId: Long,
    val before: Int,
    val after: Int
)
