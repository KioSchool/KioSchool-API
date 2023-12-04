package com.kioschool.kioschoolapi.workspace.dto

data class InviteWorkspaceRequestBody(
    val workspaceId: Long,
    val userLoginId: String,
)