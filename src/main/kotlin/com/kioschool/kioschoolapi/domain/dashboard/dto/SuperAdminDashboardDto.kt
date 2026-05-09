package com.kioschool.kioschoolapi.domain.dashboard.dto

data class SuperAdminDashboardDto(
    val users: UserStats,
    val workspaces: WorkspaceStats,
    val revenue: RevenueStats,
    val insights: Insights
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

    data class DailyPoint(
        val date: String,
        val revenue: Long,
        val orders: Int
    )

    data class WorkspaceRankItem(
        val workspaceId: Long,
        val workspaceName: String,
        val revenue: Long,
        val orders: Long
    )

    data class Funnel(
        val totalUsers: Long,
        val workspacesCreated: Long,
        val onboardingCompleted: Long,
        val hadFirstOrder: Long
    )

    data class Insights(
        val dailyLast30Days: List<DailyPoint>,
        val activeWorkspacesLast7Days: Long,
        val activeWorkspacesLast30Days: Long,
        val averageOrderValue: Long,
        val cancelledOrdersLast30Days: Long,
        val totalOrdersForCancelRate: Long,
        val topWorkspaces: List<WorkspaceRankItem>,
        val funnel: Funnel
    )
}
