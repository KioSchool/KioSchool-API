package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class UpdateOrderSettingRequestBody(
    override val workspaceId: Long,
    val useOrderSessionTimeLimit: Boolean,
    val orderSessionTimeLimitMinutes: Long,
) : WorkspaceAware