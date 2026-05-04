package com.kioschool.kioschoolapi.domain.dashboard.dto

data class SuperAdminDashboardDto(
    val users: UserStats,
    val workspaces: WorkspaceStats,
    val revenue: RevenueStats
) {
    data class UserStats(
        val total: Long,
        val newLast7Days: Long,
        val newLast30Days: Long
    )

    data class WorkspaceStats(
        val total: Long,
        val newLast7Days: Long,
        val newLast30Days: Long,
        val onboardingCompletionRate: Double
    )

    data class RevenueStats(
        val totalRevenueAllTime: Long,
        val totalOrdersAllTime: Long,
        val totalRevenueLast30Days: Long,
        val totalOrdersLast30Days: Long
    )
}
