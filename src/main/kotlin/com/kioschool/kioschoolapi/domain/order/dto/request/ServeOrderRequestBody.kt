package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class ServeOrderRequestBody(
    override val workspaceId: Long,
    val orderId: Long
) : WorkspaceAware