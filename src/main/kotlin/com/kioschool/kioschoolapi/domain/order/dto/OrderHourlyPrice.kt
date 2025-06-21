package com.kioschool.kioschoolapi.domain.order.dto

import java.time.LocalDateTime

class OrderHourlyPrice(
    val timeBucket: LocalDateTime,
    val price: Long,
)