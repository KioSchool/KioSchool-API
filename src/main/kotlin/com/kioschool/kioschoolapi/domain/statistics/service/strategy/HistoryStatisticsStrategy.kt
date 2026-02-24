package com.kioschool.kioschoolapi.domain.statistics.service.strategy

import com.kioschool.kioschoolapi.domain.statistics.dto.DailyOrderStatisticResponse
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class HistoryStatisticsStrategy(
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository,
    private val statisticsCalculator: StatisticsCalculator
) : OrderStatisticsStrategy {

    override fun supports(targetDate: LocalDate, currentBusinessDate: LocalDate): Boolean {
        return targetDate.isBefore(currentBusinessDate) // 과거 영업일 지원
    }

    @Cacheable(cacheNames = [CacheNames.HISTORY_STATISTICS], key = "#workspaceId + '-' + #date.toString()", unless = "#result == null")
    override fun getStatistics(workspaceId: Long, date: LocalDate): DailyOrderStatisticResponse {
        val entity = dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(workspaceId, date)
            .orElseGet {
                val newStatistic = statisticsCalculator.calculate(workspaceId, date)
                dailyOrderStatisticRepository.save(newStatistic)
            }
        
        return DailyOrderStatisticResponse.fromEntity(entity, isRealTime = false)
    }
}
