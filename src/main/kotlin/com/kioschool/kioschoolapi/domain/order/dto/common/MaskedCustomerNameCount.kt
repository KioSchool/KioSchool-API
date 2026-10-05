package com.kioschool.kioschoolapi.domain.order.dto.common

data class MaskedCustomerNameCount(
    val orders: Int,
    val orderSessions: Int
) {
    val total: Int
        get() = orders + orderSessions
}
