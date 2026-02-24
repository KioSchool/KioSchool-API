package com.kioschool.kioschoolapi.domain.statistics.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시간대별 매출 추이")
data class SalesByHour(
    @Schema(description = "시간대 (0~23)")
    val hour: Int,
    @Schema(description = "해당 시간대의 주문 건수")
    val orderCount: Int,
    @Schema(description = "해당 시간대의 매출액")
    val revenue: Long
)
