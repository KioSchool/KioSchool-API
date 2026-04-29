package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class CreateOrderRequestBody(
    override val workspaceId: Long,
    // todo remove after use tableHash
    val tableNumber: Int,
    val tableHash: String?,
    val orderProducts: List<OrderProductRequestBody>,
    val customerName: String
) : WorkspaceAware