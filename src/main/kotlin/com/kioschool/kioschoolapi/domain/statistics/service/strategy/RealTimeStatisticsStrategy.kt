package com.kioschool.kioschoolapi.domain.statistics.service.strategy

import com.kioschool.kioschoolapi.domain.statistics.dto.DailyOrderStatisticResponse
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RealTimeStatisticsStrategy(
    private val statisticsCalculator: StatisticsCalculator
) : OrderStatisticsStrategy {

    override fun supports(targetDate: LocalDate, currentBusinessDate: LocalDate): Boolean {
        return !targetDate.isBefore(currentBusinessDate) // 당일 영업일 지원
    }

    @Cacheable(cacheNames = ["${CacheNames.REAL_TIME_STATISTICS}#5m"], key = "#workspaceId + '-' + #date.toString()", unless = "#result == null")
    override fun getStatistics(workspaceId: Long, date: LocalDate): DailyOrderStatisticResponse {
        val entity = statisticsCalculator.calculate(workspaceId, date)
        return DailyOrderStatisticResponse.fromEntity(entity, isRealTime = true)
    }
}
