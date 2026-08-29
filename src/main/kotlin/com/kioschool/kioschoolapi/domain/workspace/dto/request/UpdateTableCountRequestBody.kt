package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateTableCountRequestBody(
    override val workspaceId: Long,
    @field:Min(1, message = "테이블 개수는 1개 이상이어야 합니다.")
    @field:Max(100, message = "테이블 개수는 100개 이하여야 합니다.")
    val tableCount: Int
) : WorkspaceAware
