package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class ResetTablePositionsRequestBody(
    override val workspaceId: Long,
) : WorkspaceAware
