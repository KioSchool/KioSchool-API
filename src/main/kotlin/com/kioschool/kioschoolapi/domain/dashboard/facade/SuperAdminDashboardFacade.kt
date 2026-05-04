package com.kioschool.kioschoolapi.domain.dashboard.facade

import com.kioschool.kioschoolapi.domain.dashboard.dto.SuperAdminDashboardDto
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class SuperAdminDashboardFacade(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository
) {
    fun getDashboard(): SuperAdminDashboardDto {
        val now = LocalDateTime.now()
        val sevenDaysAgo = now.minusDays(7)
        val thirtyDaysAgo = now.minusDays(30)
        val thirtyDaysAgoDate = LocalDate.now().minusDays(30)

        val totalUsers = userRepository.count()
        val newUsersLast7Days = userRepository.countByCreatedAtAfter(sevenDaysAgo)
        val newUsersLast30Days = userRepository.countByCreatedAtAfter(thirtyDaysAgo)

        val totalWorkspaces = workspaceRepository.count()
        val newWorkspacesLast7Days = workspaceRepository.countByCreatedAtAfter(sevenDaysAgo)
        val newWorkspacesLast30Days = workspaceRepository.countByCreatedAtAfter(thirtyDaysAgo)
        val onboardingCompleted = workspaceRepository.countByIsOnboardingFalse()
        val onboardingRate = if (totalWorkspaces > 0) onboardingCompleted.toDouble() / totalWorkspaces else 0.0

        val totalRevenue = dailyOrderStatisticRepository.sumTotalRevenue()
        val totalOrders = dailyOrderStatisticRepository.sumTotalOrders()
        val revenueLast30Days = dailyOrderStatisticRepository.sumTotalRevenueSince(thirtyDaysAgoDate)
        val ordersLast30Days = dailyOrderStatisticRepository.sumTotalOrdersSince(thirtyDaysAgoDate)

        return SuperAdminDashboardDto(
            users = SuperAdminDashboardDto.UserStats(
                total = totalUsers,
                newLast7Days = newUsersLast7Days,
                newLast30Days = newUsersLast30Days
            ),
            workspaces = SuperAdminDashboardDto.WorkspaceStats(
                total = totalWorkspaces,
                newLast7Days = newWorkspacesLast7Days,
                newLast30Days = newWorkspacesLast30Days,
                onboardingCompletionRate = onboardingRate
            ),
            revenue = SuperAdminDashboardDto.RevenueStats(
                totalRevenueAllTime = totalRevenue,
                totalOrdersAllTime = totalOrders,
                totalRevenueLast30Days = revenueLast30Days,
                totalOrdersLast30Days = ordersLast30Days
            )
        )
    }
}
