package com.kioschool.kioschoolapi.domain.account.dto.common

data class AccountConnectionStatusDto(
    val totalUsers: Long,
    val usersWithAccount: Long,
    val usersWithoutAccount: Long,
    val connectionRate: Double
)
