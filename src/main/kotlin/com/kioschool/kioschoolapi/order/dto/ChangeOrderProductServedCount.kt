package com.kioschool.kioschoolapi.order.dto

class ChangeOrderProductServedCount(
    val workspaceId: Long,
    val orderProductId: Long,
    val servedCount: Int
)