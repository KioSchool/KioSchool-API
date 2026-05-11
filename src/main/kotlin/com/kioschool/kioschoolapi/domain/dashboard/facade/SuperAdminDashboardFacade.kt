package com.kioschool.kioschoolapi.domain.dashboard.facade

import com.kioschool.kioschoolapi.domain.dashboard.dto.SuperAdminDashboardDto
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class SuperAdminDashboardFacade(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository,
    private val orderRepository: OrderRepository
) {
    fun getDashboard(): SuperAdminDashboardDto {
        val now = LocalDateTime.now()
        val sevenDaysAgo = now.minusDays(7)
        val thirtyDaysAgo = now.minusDays(30)
        val sevenDaysAgoDate = LocalDate.now().minusDays(7)
        val thirtyDaysAgoDate = LocalDate.now().minusDays(30)

        val totalUsers = userRepository.count()
        val newUsersLast7Days = userRepository.countByCreatedAtAfter(sevenDaysAgo)
        val newUsersLast30Days = userRepository.countByCreatedAtAfter(thirtyDaysAgo)

        val totalWorkspaces = workspaceRepository.countByMembersNotEmpty()
        val newWorkspacesLast7Days = workspaceRepository.countByCreatedAtAfter(sevenDaysAgo)
        val newWorkspacesLast30Days = workspaceRepository.countByCreatedAtAfter(thirtyDaysAgo)
        val onboardingCompleted = workspaceRepository.countOnboardingCompletedWithMembers()
        val onboardingRate = if (totalWorkspaces > 0) onboardingCompleted.toDouble() / totalWorkspaces else 0.0

        val totalRevenue = dailyOrderStatisticRepository.sumTotalRevenue()
        val totalOrders = dailyOrderStatisticRepository.sumTotalOrders()
        val revenueLast30Days = dailyOrderStatisticRepository.sumTotalRevenueSince(thirtyDaysAgoDate)
        val ordersLast30Days = dailyOrderStatisticRepository.sumTotalOrdersSince(thirtyDaysAgoDate)

        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dailyLast30Days = dailyOrderStatisticRepository.findAllSince(thirtyDaysAgoDate)
            .groupBy { it.referenceDate }
            .map { (date, stats) ->
                SuperAdminDashboardDto.DailyPoint(
                    date = date.format(dateFormatter),
                    revenue = stats.sumOf { it.totalRevenue },
                    orders = stats.sumOf { it.totalOrders }
                )
            }
            .sortedBy { it.date }

        val activeWorkspacesLast7Days = dailyOrderStatisticRepository.countActiveWorkspacesSince(sevenDaysAgoDate)
        val activeWorkspacesLast30Days = dailyOrderStatisticRepository.countActiveWorkspacesSince(thirtyDaysAgoDate)

        val aov = if (totalOrders > 0) totalRevenue / totalOrders else 0L

        val cancelledLast30Days = orderRepository.countByStatusAndCreatedAtAfter(
            OrderStatus.CANCELLED, thirtyDaysAgo
        )
        val totalOrdersForCancelRate = orderRepository.countByCreatedAtAfter(thirtyDaysAgo)

        val topRaw = dailyOrderStatisticRepository.findTopWorkspacesByRevenueSince(thirtyDaysAgoDate)
        val topWorkspaceIds = topRaw.take(5).map { (it[0] as Number).toLong() }
        val workspaceMap = workspaceRepository.findAllById(topWorkspaceIds).associateBy { it.id }
        val topWorkspaces = topRaw.take(5).mapNotNull { row ->
            val wsId = (row[0] as Number).toLong()
            val ws = workspaceMap[wsId] ?: return@mapNotNull null
            SuperAdminDashboardDto.WorkspaceRankItem(
                workspaceId = wsId,
                workspaceName = ws.name,
                revenue = (row[1] as Number).toLong(),
                orders = (row[2] as Number).toLong()
            )
        }

        val onboardingTimeStats = buildOnboardingTimeStats(totalUsers)

        val workspacesWithOrder = dailyOrderStatisticRepository.countActiveWorkspacesSince(
            LocalDate.of(2000, 1, 1)
        )
        val funnel = SuperAdminDashboardDto.Funnel(
            totalUsers = totalUsers,
            workspacesCreated = totalWorkspaces,
            onboardingCompleted = onboardingCompleted,
            hadFirstOrder = workspacesWithOrder
        )

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
            ),
            insights = SuperAdminDashboardDto.Insights(
                dailyLast30Days = dailyLast30Days,
                activeWorkspacesLast7Days = activeWorkspacesLast7Days,
                activeWorkspacesLast30Days = activeWorkspacesLast30Days,
                averageOrderValue = aov,
                cancelledOrdersLast30Days = cancelledLast30Days,
                totalOrdersForCancelRate = totalOrdersForCancelRate,
                topWorkspaces = topWorkspaces,
                funnel = funnel,
                onboardingTimeStats = onboardingTimeStats
            )
        )
    }

    private fun buildOnboardingTimeStats(totalUsers: Long): SuperAdminDashboardDto.OnboardingTimeStats {
        val rows = workspaceRepository.findUserRegistrationAndFirstWorkspaceCreatedAt()
        val neverCreatedCount = totalUsers - rows.size

        val minutesList = rows.mapNotNull { row ->
            val userCreatedAt = row[0] as? LocalDateTime ?: return@mapNotNull null
            val firstWorkspaceCreatedAt = row[1] as? LocalDateTime ?: return@mapNotNull null
            java.time.Duration.between(userCreatedAt, firstWorkspaceCreatedAt).toMinutes().toDouble()
        }.filter { it >= 0 }.sorted()

        if (minutesList.isEmpty()) {
            return SuperAdminDashboardDto.OnboardingTimeStats(
                averageMinutes = 0.0,
                medianMinutes = 0.0,
                neverCreatedCount = neverCreatedCount,
                distribution = BUCKET_DEFINITIONS.map { SuperAdminDashboardDto.DistributionBucket(it.first, 0) }
            )
        }

        val average = minutesList.average()
        val median = if (minutesList.size % 2 == 0) {
            (minutesList[minutesList.size / 2 - 1] + minutesList[minutesList.size / 2]) / 2.0
        } else {
            minutesList[minutesList.size / 2]
        }

        val distribution = BUCKET_DEFINITIONS.map { (label, min, max) ->
            val count = minutesList.count { it >= min && it < max }
            SuperAdminDashboardDto.DistributionBucket(label, count)
        }

        return SuperAdminDashboardDto.OnboardingTimeStats(
            averageMinutes = average,
            medianMinutes = median,
            neverCreatedCount = neverCreatedCount,
            distribution = distribution
        )
    }

    companion object {
        // Triple: label, minMinutes (inclusive), maxMinutes (exclusive)
        private val BUCKET_DEFINITIONS = listOf(
            Triple("10분 미만", 0.0, 10.0),
            Triple("10~30분", 10.0, 30.0),
            Triple("30분~1시간", 30.0, 60.0),
            Triple("1~6시간", 60.0, 360.0),
            Triple("6~24시간", 360.0, 1440.0),
            Triple("1~3일", 1440.0, 4320.0),
            Triple("3~7일", 4320.0, 10080.0),
            Triple("7일 초과", 10080.0, Double.MAX_VALUE)
        )
    }
}
