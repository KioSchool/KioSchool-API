package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class LeaveWorkspaceRequestBody(
    override val workspaceId: Long
) : WorkspaceAware