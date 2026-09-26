package com.kioschool.kioschoolapi.domain.user.dto.common

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountDto
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import java.time.LocalDateTime

data class SuperAdminUserDto(
    val id: Long,
    val loginId: String,
    val name: String,
    val email: String?,
    val schoolName: String,
    val role: UserRole,
    val account: AccountDto?,
    val workspaces: List<SuperAdminUserWorkspaceDto>,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun of(user: User, schoolName: String): SuperAdminUserDto {
            return SuperAdminUserDto(
                id = user.id,
                loginId = user.loginId,
                name = user.name,
                email = user.email,
                schoolName = schoolName,
                role = user.role,
                account = user.account?.let { AccountDto.of(it) },
                workspaces = user.getWorkspaces().map {
                    SuperAdminUserWorkspaceDto(id = it.id, name = it.name, isOwner = it.owner.id == user.id)
                },
                createdAt = user.createdAt
            )
        }
    }
}

data class SuperAdminUserWorkspaceDto(
    val id: Long,
    val name: String,
    val isOwner: Boolean
)
