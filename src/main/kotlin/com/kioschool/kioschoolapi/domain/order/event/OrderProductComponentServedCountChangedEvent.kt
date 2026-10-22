package com.kioschool.kioschoolapi.domain.order.event

data class OrderProductComponentServedCountChangedEvent(
    val workspaceId: Long,
    val orderProductComponentId: Long,
    val before: Int,
    val after: Int
)
