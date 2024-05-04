package com.kioschool.kioschoolapi.order.dto

data class ServeOrderProductRequestBody(
    val workspaceId: Long,
    val orderProductId: Long,
    val isServed: Boolean
)
