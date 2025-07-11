package com.kioschool.kioschoolapi.domain.order.dto

import java.time.LocalDateTime

class UpdateExpectedEndAtRequestBody(
    val workspaceId: Long,
    val orderSessionId: Long,
    val expectedEndAt: LocalDateTime
)