package com.kioschool.kioschoolapi.domain.order.event

import java.time.LocalDateTime

data class OrderSessionExpectedEndAtChangedEvent(
    val workspaceId: Long,
    val orderSessionId: Long,
    val before: LocalDateTime?,
    val after: LocalDateTime?
)
