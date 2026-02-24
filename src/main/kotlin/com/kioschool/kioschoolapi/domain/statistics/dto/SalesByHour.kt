package com.kioschool.kioschoolapi.domain.statistics.dto

data class SalesByHour(
    val hour: Int,
    val orderCount: Int,
    val revenue: Long
)
