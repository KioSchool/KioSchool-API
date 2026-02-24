package com.kioschool.kioschoolapi.domain.statistics.entity

import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProducts
import com.kioschool.kioschoolapi.domain.statistics.dto.PreviousDayComparison
import com.kioschool.kioschoolapi.domain.statistics.dto.SalesByHour
import com.kioschool.kioschoolapi.domain.statistics.entity.converter.PopularProductsConverter
import com.kioschool.kioschoolapi.domain.statistics.entity.converter.PreviousDayComparisonConverter
import com.kioschool.kioschoolapi.domain.statistics.entity.converter.SalesByHourListConverter
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "daily_order_statistic")
class DailyOrderStatistic(
    @ManyToOne(fetch = FetchType.LAZY)
    val workspace: Workspace,

    val referenceDate: LocalDate,
    val totalSalesVolume: Int,
    val totalRevenue: Long,
    val averageOrderAmount: Int,
    val totalOrders: Int,
    val averageOrdersPerTable: Double,
    val tableTurnoverRate: Double,
    val averageStayTimeMinutes: Double,

    @Convert(converter = PreviousDayComparisonConverter::class)
    @Column(columnDefinition = "TEXT")
    val previousDayComparison: PreviousDayComparison?,

    @Convert(converter = SalesByHourListConverter::class)
    @Column(columnDefinition = "TEXT")
    val salesByHour: List<SalesByHour>,

    @Convert(converter = PopularProductsConverter::class)
    @Column(columnDefinition = "TEXT")
    val popularProducts: PopularProducts
) : BaseEntity()
