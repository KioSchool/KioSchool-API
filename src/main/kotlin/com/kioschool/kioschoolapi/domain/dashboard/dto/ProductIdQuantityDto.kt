package com.kioschool.kioschoolapi.domain.dashboard.dto

data class ProductIdQuantityDto(
    val productId: Long,
    val productName: String,
    val totalQuantity: Long
)
