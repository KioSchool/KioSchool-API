package com.kioschool.kioschoolapi.domain.insight.entity

data class CardPayload(
    val totalRevenue: Long? = null,
    val totalOrders: Int? = null,
    val averageOrderAmount: Int? = null,
    val tableCount: Int? = null,
    val averageStayMinutes: Double? = null,
    val cohortAverageRatio: Double? = null,
    val absoluteValue: Number? = null,
    val milestoneStep: Long? = null
)
