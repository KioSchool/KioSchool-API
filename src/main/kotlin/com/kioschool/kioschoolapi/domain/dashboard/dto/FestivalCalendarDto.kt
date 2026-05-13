package com.kioschool.kioschoolapi.domain.dashboard.dto

data class FestivalCalendarDto(
    val monthSummary: MonthSummary,
    val universityBreakdown: List<UniversityStats>,
    val workspaceRanking: List<WorkspaceRankItem>,
    val calendar: Map<String, List<FestivalWorkspace>>
) {
    data class MonthSummary(
        val totalFestivalDays: Int,
        val uniqueUniversities: Int,
        val totalOrders: Long,
        val totalRevenue: Long,
        val busiestDay: String?
    )

    data class UniversityStats(
        val universityName: String,
        val festivalDays: Int,
        val totalOrders: Long,
        val totalRevenue: Long
    )

    data class WorkspaceRankItem(
        val workspaceId: Long,
        val workspaceName: String,
        val universityName: String,
        val festivalDays: Int,
        val totalOrders: Long,
        val totalRevenue: Long,
        val averageOrderAmount: Int
    )

    data class FestivalWorkspace(
        val workspaceId: Long,
        val workspaceName: String,
        val universityName: String,
        val totalOrders: Int,
        val totalRevenue: Long,
        val averageOrderAmount: Int,
        val tableTurnoverRate: Double,
        val averageStayTimeMinutes: Double,
        val peakHour: Int?
    )
}
