package com.kioschool.kioschoolapi.domain.order.dto.request

data class ServeOrderProductRequestBody(
    val workspaceId: Long,
    val orderProductId: Long,
    val isServed: Boolean
)
