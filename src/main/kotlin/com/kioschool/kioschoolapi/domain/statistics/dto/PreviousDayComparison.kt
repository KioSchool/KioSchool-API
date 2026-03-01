package com.kioschool.kioschoolapi.domain.statistics.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전일 대비 증감 지표")
data class PreviousDayComparison(
    @Schema(description = "전일 대비 매출 증감률 (%)")
    val revenueGrowthRate: Double,
    @Schema(description = "전일 대비 주문 건수 증감 차이 (건)")
    val orderCountDifference: Int
)
