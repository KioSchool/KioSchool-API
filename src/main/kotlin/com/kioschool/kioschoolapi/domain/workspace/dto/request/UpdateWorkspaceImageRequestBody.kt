package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import jakarta.validation.constraints.Size

class UpdateWorkspaceImageRequestBody(
    override val workspaceId: Long,
    @Size(min = 3, max = 3, message = "이미지 아이디는 3개여야 합니다.")
    val imageIds: List<Long?>
) : WorkspaceAware