package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class StartOrderSessionRequestBody(
    override val workspaceId: Long,
    val tableNumber: Int,
) : WorkspaceAware