package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import java.time.LocalDateTime

data class WorkspaceAdminDetailDto(
    val id: Long,
    val name: String,
    val owner: UserDto,
    val description: String,
    val notice: String,
    val memo: String,
    val tableCount: Int,
    val memberCount: Int,
    val productCount: Int,
    val isOnboarding: Boolean,
    val workspaceSetting: WorkspaceSettingDto,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(workspace: Workspace): WorkspaceAdminDetailDto {
            return WorkspaceAdminDetailDto(
                id = workspace.id,
                name = workspace.name,
                owner = UserDto.of(workspace.owner),
                description = workspace.description,
                notice = workspace.notice,
                memo = workspace.memo,
                tableCount = workspace.tableCount,
                memberCount = workspace.members.size,
                productCount = workspace.products.size,
                isOnboarding = workspace.isOnboarding,
                workspaceSetting = WorkspaceSettingDto.of(workspace.workspaceSetting),
                createdAt = workspace.createdAt,
                updatedAt = workspace.updatedAt
            )
        }
    }
}
