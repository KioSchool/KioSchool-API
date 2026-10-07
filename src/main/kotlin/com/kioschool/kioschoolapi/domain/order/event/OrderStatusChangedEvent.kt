package com.kioschool.kioschoolapi.domain.order.event

import com.kioschool.kioschoolapi.global.common.enums.OrderStatus

data class OrderStatusChangedEvent(
    val workspaceId: Long,
    val orderId: Long,
    val before: OrderStatus,
    val after: OrderStatus
)
