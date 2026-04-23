package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class JoinWorkspaceRequestBody(
    override val workspaceId: Long
) : WorkspaceAware