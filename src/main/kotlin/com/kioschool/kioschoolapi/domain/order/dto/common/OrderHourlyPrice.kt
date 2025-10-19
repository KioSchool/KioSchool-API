package com.kioschool.kioschoolapi.domain.order.dto.common

import java.time.LocalDateTime

class OrderHourlyPrice(
    val timeBucket: LocalDateTime,
    val price: Long,
)