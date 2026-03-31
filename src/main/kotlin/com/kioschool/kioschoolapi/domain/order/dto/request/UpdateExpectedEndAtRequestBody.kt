package com.kioschool.kioschoolapi.domain.order.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import java.time.LocalDateTime

class UpdateExpectedEndAtRequestBody(
    override val workspaceId: Long,
    val orderSessionId: Long,
    val expectedEndAt: LocalDateTime
) : WorkspaceAware