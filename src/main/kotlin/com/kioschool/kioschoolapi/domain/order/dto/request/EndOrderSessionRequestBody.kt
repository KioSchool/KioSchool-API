package com.kioschool.kioschoolapi.domain.order.dto.request

class EndOrderSessionRequestBody(
    val workspaceId: Long,
    val tableNumber: Int,
    val orderSessionId: Long
)