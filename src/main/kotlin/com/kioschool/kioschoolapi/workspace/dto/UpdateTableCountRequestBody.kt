package com.kioschool.kioschoolapi.workspace.dto

import org.hibernate.validator.constraints.Length

class UpdateTableCountRequestBody(
    val workspaceId: Long,
    @field:Length(min = 0, max = 100, message = "테이블은 0 ~ 100 사이로 입력해주세요.")
    val tableCount: Int
)