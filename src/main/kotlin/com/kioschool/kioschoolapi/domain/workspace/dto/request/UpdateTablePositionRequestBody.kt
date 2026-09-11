package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionDto
import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class UpdateTablePositionRequestBody(
    override val workspaceId: Long,
    val tableId: Long,
    val position: TablePositionDto?,
) : WorkspaceAware
