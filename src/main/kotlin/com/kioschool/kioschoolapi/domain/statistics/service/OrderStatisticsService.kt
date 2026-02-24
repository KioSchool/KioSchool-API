package com.kioschool.kioschoolapi.domain.statistics.service

import com.kioschool.kioschoolapi.domain.statistics.dto.DailyOrderStatisticResponse
import com.kioschool.kioschoolapi.domain.statistics.service.strategy.OrderStatisticsStrategy
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OrderStatisticsService(
    private val strategies: List<OrderStatisticsStrategy>
) {
    fun getStatistics(workspaceId: Long, date: LocalDate?): DailyOrderStatisticResponse {
        val now = LocalDateTime.now()
        val currentBusinessDate = if (now.hour < 9) now.toLocalDate().minusDays(1) else now.toLocalDate()
        
        val targetDate = date ?: currentBusinessDate

        val strategy = strategies.firstOrNull { it.supports(targetDate, currentBusinessDate) }
            ?: throw IllegalArgumentException("No suitable statistics strategy found for the requested date")
            
        return strategy.getStatistics(workspaceId, targetDate)
    }
}
