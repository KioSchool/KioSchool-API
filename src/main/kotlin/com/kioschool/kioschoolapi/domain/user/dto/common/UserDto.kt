package com.kioschool.kioschoolapi.domain.user.dto.common

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountDto
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import java.time.LocalDateTime

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val accountUrl: String?,
    val account: AccountDto?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(user: User): UserDto {
            return UserDto(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role,
                accountUrl = user.accountUrl,
                account = user.account?.let { AccountDto.of(it) },
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
