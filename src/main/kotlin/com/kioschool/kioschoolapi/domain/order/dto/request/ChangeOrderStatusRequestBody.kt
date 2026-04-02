package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class ChangeOrderStatusRequestBody(
    override val workspaceId: Long,
    val orderId: Long,
    val status: String
) : WorkspaceAware
