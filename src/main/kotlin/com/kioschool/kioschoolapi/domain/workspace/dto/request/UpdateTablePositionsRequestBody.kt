package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionUpdateDto
import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import jakarta.validation.constraints.Size

data class UpdateTablePositionsRequestBody(
    override val workspaceId: Long,
    // 워크스페이스당 테이블 상한(UpdateTableCountRequestBody의 @Max(100))과 맞춘다.
    @field:Size(max = 100, message = "한 번에 100개 테이블까지 저장할 수 있습니다.")
    val positions: List<TablePositionUpdateDto>
) : WorkspaceAware
