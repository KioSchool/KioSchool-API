package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import java.time.LocalDateTime

data class SuperAdminWorkspaceDto(
    val id: Long,
    val name: String,
    val owner: WorkspaceUserSummaryDto,
    val isOnboarding: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(workspace: Workspace): SuperAdminWorkspaceDto {
            return SuperAdminWorkspaceDto(
                id = workspace.id,
                name = workspace.name,
                owner = WorkspaceUserSummaryDto.of(workspace.owner),
                isOnboarding = workspace.isOnboarding,
                createdAt = workspace.createdAt,
                updatedAt = workspace.updatedAt
            )
        }
    }
}
