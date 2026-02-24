package com.kioschool.kioschoolapi.domain.statistics.dto

data class PopularProductItem(
    val productId: Long,
    val name: String,
    val value: Double
)

data class PopularProducts(
    val byQuantity: List<PopularProductItem>,
    val byReorderRate: List<PopularProductItem>,
    val byRevenue: List<PopularProductItem>
)
