package com.kioschool.kioschoolapi.workspace.dto

class UpdateWorkspaceRequestBody(
    val workspaceId: Long,
    val name: String,
    val description: String,
    val notice: String,
    val imageUrl1: String?,
    val imageUrl2: String?,
    val imageUrl3: String?,
)