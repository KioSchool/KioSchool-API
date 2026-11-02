package com.kioschool.kioschoolapi.domain.order.dto.common

import java.time.LocalDateTime

data class WorkspaceOrderStats(
    val workspaceId: Long,
    val orderCount: Long,
    val revenue: Long,
    val operatingDays: Long,
    val firstOrderAt: LocalDateTime,
    val lastOrderAt: LocalDateTime,
)
