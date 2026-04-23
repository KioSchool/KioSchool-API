package com.kioschool.kioschoolapi.domain.statistics.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "일별 주문 통계 응답")
data class DailyOrderStatisticResponse(
    @Schema(description = "기준 영업일 (해당 일자 09:00 ~ 익일 08:59 기준)")
    val referenceDate: LocalDate,
    @Schema(description = "총 판매된 상품 수량")
    val totalSalesVolume: Int,
    @Schema(description = "해당 영업일의 총 매출액")
    val totalRevenue: Long,
    @Schema(description = "평균 주문 금액 (전체 매출 / 전체 주문 수)")
    val averageOrderAmount: Int,
    @Schema(description = "총 주문 결제 건수")
    val totalOrders: Int,
    @Schema(description = "테이블 당 평균 주문 건수")
    val averageOrdersPerTable: Double,
    @Schema(description = "테이블 회전율 (총 주문 세션 수 / 전체 테이블 수)")
    val tableTurnoverRate: Double,
    @Schema(description = "평균 체류 시간 (분 단위, 결제완료/주문세션 종료 기준)")
    val averageStayTimeMinutes: Double,
    @Schema(description = "전일 대비 증감 지표 (매출 증감률 및 주문 수 차이)")
    val previousDayComparison: PreviousDayComparison?,
    @Schema(description = "시간대별 매출 및 주문 건수 추이 (09시부터 익일 08시까지)")
    val salesByHour: List<SalesByHour>,
    @Schema(description = "인기 상품 순위 랭킹 (판매량, 재주문율, 매출액 기준)")
    val popularProducts: PopularProducts,
    @Schema(description = "현시점 실시간 집계 여부 (true: 당일 실시간 집계, false: 과거 확정 통계)")
    val isRealTime: Boolean,
    @Schema(description = "통계 최종 업데이트 시간")
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
