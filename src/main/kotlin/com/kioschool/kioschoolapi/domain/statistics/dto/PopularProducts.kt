package com.kioschool.kioschoolapi.domain.statistics.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "인기 상품 단일 항목")
data class PopularProductItem(
    @Schema(description = "상품 ID")
    val productId: Long,
    @Schema(description = "상품명")
    val name: String,
    @Schema(description = "지표 값 (판매량, 비율, 매출액 등)")
    val value: Double
)

@Schema(description = "기준별 인기 상품 랭킹")
data class PopularProducts(
    @Schema(description = "판매량 기준 인기 상품 순위")
    val byQuantity: List<PopularProductItem>,
    @Schema(description = "재주문율 기준 인기 상품 순위")
    val byReorderRate: List<PopularProductItem>,
    @Schema(description = "매출액 기준 인기 상품 순위")
    val byRevenue: List<PopularProductItem>
)
