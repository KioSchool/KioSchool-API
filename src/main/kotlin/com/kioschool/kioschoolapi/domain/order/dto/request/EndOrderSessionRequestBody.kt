package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class EndOrderSessionRequestBody(
    override val workspaceId: Long,
    val tableNumber: Int,
    val orderSessionId: Long,
    val isGhost: Boolean? = null
) : WorkspaceAware