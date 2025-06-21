package com.kioschool.kioschoolapi.domain.order.dto

class ChangeOrderProductServedCount(
    val workspaceId: Long,
    val orderProductId: Long,
    val servedCount: Int
)