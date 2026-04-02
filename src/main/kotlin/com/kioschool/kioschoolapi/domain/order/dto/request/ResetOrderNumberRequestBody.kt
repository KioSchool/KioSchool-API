package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class ResetOrderNumberRequestBody(
    override val workspaceId: Long
) : WorkspaceAware
