package com.kioschool.kioschoolapi.domain.statistics.facade

import com.kioschool.kioschoolapi.domain.statistics.dto.DailyOrderStatisticResponse
import com.kioschool.kioschoolapi.domain.statistics.service.OrderStatisticsService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OrderStatisticsFacade(
    private val orderStatisticsService: OrderStatisticsService,
    private val workspaceService: WorkspaceService
) {
    fun getStatistics(username: String, workspaceId: Long, date: LocalDate?): DailyOrderStatisticResponse {
        workspaceService.checkAccessible(username, workspaceId)
        return orderStatisticsService.getStatistics(workspaceId, date)
    }
}
