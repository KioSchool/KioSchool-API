package com.kioschool.kioschoolapi.domain.statistics.service.strategy

import com.kioschool.kioschoolapi.domain.statistics.dto.DailyOrderStatisticResponse
import java.time.LocalDate

interface OrderStatisticsStrategy {
    fun supports(targetDate: LocalDate, currentBusinessDate: LocalDate): Boolean
    fun getStatistics(workspaceId: Long, date: LocalDate): DailyOrderStatisticResponse
}
