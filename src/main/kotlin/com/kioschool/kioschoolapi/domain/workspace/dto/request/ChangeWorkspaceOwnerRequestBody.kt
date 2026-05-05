package com.kioschool.kioschoolapi.domain.workspace.dto.request

data class ChangeWorkspaceOwnerRequestBody(
    val workspaceId: Long,
    val newOwnerLoginId: String
)
