package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class UpdateWorkspaceRequestBody(
    override val workspaceId: Long,
    val name: String,
    val description: String,
    val notice: String,
) : WorkspaceAware