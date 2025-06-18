package com.kioschool.kioschoolapi.domain.order.dto

import java.time.LocalDateTime

class OrderPrefixSumPrice(
    val timeBucket: LocalDateTime,
    val prefixSumPrice: Long,
)