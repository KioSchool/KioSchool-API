package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.workspace.entity.Workspace

data class GetWorkspacesResponseBody(
    val workspaces: List<Workspace>
)