package com.kioschool.kioschoolapi.domain.account.dto.common

data class AccountConnectionStatusDto(
    val totalUsers: Long,
    val usersWithAccount: Long,
    val usersWithoutAccount: Long,
    val connectionRate: Double,
    val usersWithToss: Long,
    val tossRateOfTotal: Double,
    val tossRateOfAccount: Double
)
