package com.kioschool.kioschoolapi.domain.order.dto

class EndOrderSessionRequestBody(
    val workspaceId: Long,
    val tableNumber: Int,
    val orderSessionId: Long
)