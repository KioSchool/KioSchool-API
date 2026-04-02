package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import org.hibernate.validator.constraints.Length

class UpdateTableCountRequestBody(
    override val workspaceId: Long,
    @field:Length(min = 1, max = 100, message = "테이블 개수는 1 ~ 100 사이로 입력해주세요.")
    val tableCount: Int
) : WorkspaceAware