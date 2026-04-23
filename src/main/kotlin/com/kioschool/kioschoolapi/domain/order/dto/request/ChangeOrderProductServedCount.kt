package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class ChangeOrderProductServedCount(
    override val workspaceId: Long,
    val orderProductId: Long,
    val servedCount: Int
) : WorkspaceAware