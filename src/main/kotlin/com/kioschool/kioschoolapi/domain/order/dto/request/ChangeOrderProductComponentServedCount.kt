package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class ChangeOrderProductComponentServedCount(
    override val workspaceId: Long,
    val orderProductComponentId: Long,
    val servedCount: Int
) : WorkspaceAware
