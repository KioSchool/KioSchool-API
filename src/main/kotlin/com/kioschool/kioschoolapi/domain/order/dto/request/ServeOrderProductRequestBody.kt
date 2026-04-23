package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class ServeOrderProductRequestBody(
    override val workspaceId: Long,
    val orderProductId: Long,
    val isServed: Boolean
) : WorkspaceAware
