package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class InviteWorkspaceRequestBody(
    override val workspaceId: Long,
    val userLoginId: String,
) : WorkspaceAware