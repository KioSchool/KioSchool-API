package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceImage
import java.time.LocalDateTime

data class WorkspaceImageDto(
    val id: Long,
    val url: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(workspaceImage: WorkspaceImage): WorkspaceImageDto {
            return WorkspaceImageDto(
                id = workspaceImage.id,
                url = workspaceImage.url,
                createdAt = workspaceImage.createdAt,
                updatedAt = workspaceImage.updatedAt
            )
        }
    }
}
