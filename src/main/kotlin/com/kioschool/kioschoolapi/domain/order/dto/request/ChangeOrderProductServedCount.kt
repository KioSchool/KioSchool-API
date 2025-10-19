package com.kioschool.kioschoolapi.domain.order.dto.request

class ChangeOrderProductServedCount(
    val workspaceId: Long,
    val orderProductId: Long,
    val servedCount: Int
)