package com.kioschool.kioschoolapi.domain.workspace.dto.request

class UpdateOrderSettingRequestBody(
    val workspaceId: Long,
    val useOrderSessionTimeLimit: Boolean,
    val orderSessionTimeLimitMinutes: Long,
)