package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

class UpdateWorkspaceMemoRequestBody(
    override val workspaceId: Long,
    val memo: String,
) : WorkspaceAware