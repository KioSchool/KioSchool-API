package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked

data class WorkspaceUserSummaryDto(
    val id: Long,
    val loginId: String,
    @LogMasked
    val name: String,
    val email: String?
) {
    companion object {
        fun of(user: User): WorkspaceUserSummaryDto {
            return WorkspaceUserSummaryDto(
                id = user.id,
                loginId = user.loginId,
                name = user.name,
                email = user.email
            )
        }
    }
}
