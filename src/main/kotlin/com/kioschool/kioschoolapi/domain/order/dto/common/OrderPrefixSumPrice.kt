package com.kioschool.kioschoolapi.domain.order.dto.common

import java.time.LocalDateTime

class OrderPrefixSumPrice(
    val timeBucket: LocalDateTime,
    val prefixSumPrice: Long,
)