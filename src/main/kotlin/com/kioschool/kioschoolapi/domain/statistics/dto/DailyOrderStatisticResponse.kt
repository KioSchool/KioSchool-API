package com.kioschool.kioschoolapi.domain.statistics.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class DailyOrderStatisticResponse(
    val referenceDate: LocalDate,
    val totalSalesVolume: Int,
    val totalRevenue: Long,
    val averageOrderAmount: Int,
    val totalOrders: Int,
    val averageOrdersPerTable: Double,
    val tableTurnoverRate: Double,
    val averageStayTimeMinutes: Double,
    val previousDayComparison: PreviousDayComparison?,
    val salesByHour: List<SalesByHour>,
    val popularProducts: PopularProducts,
    val isRealTime: Boolean,
    val lastUpdated: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic, isRealTime: Boolean): DailyOrderStatisticResponse {
            return DailyOrderStatisticResponse(
                referenceDate = entity.referenceDate,
                totalSalesVolume = entity.totalSalesVolume,
                totalRevenue = entity.totalRevenue,
                averageOrderAmount = entity.averageOrderAmount,
                totalOrders = entity.totalOrders,
                averageOrdersPerTable = entity.averageOrdersPerTable,
                tableTurnoverRate = entity.tableTurnoverRate,
                averageStayTimeMinutes = entity.averageStayTimeMinutes,
                previousDayComparison = entity.previousDayComparison,
                salesByHour = entity.salesByHour,
                popularProducts = entity.popularProducts,
                isRealTime = isRealTime,
                lastUpdated = entity.updatedAt ?: LocalDateTime.now()
            )
        }
    }
}
