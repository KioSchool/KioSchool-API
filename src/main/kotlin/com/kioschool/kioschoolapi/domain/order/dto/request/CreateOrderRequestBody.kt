package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.enums.PaymentMethod
import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class CreateOrderRequestBody(
    override val workspaceId: Long,
    val tableHash: String?,
    val orderProducts: List<OrderProductRequestBody>,
    val customerName: String,
    // 구버전 프론트와 0원 주문은 보내지 않는다.
    val paymentMethod: PaymentMethod? = null
) : WorkspaceAware
