package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import java.time.LocalDateTime

data class WorkspaceSettingDto(
    val id: Long,
    val useOrderSessionTimeLimit: Boolean,
    val orderSessionTimeLimitMinutes: Long,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(workspaceSetting: WorkspaceSetting): WorkspaceSettingDto {
            return WorkspaceSettingDto(
                id = workspaceSetting.id,
                useOrderSessionTimeLimit = workspaceSetting.useOrderSessionTimeLimit,
                orderSessionTimeLimitMinutes = workspaceSetting.orderSessionTimeLimitMinutes,
                createdAt = workspaceSetting.createdAt,
                updatedAt = workspaceSetting.updatedAt
            )
        }
    }
}
