package com.kioschool.kioschoolapi.domain.workspace.dto.request

data class InviteWorkspaceRequestBody(
    val workspaceId: Long,
    val userLoginId: String,
)