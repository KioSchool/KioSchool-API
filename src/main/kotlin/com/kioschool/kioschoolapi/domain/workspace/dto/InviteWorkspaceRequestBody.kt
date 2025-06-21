package com.kioschool.kioschoolapi.domain.workspace.dto

data class InviteWorkspaceRequestBody(
    val workspaceId: Long,
    val userLoginId: String,
)