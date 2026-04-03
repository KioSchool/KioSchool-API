package com.kioschool.kioschoolapi.domain.order.event

import com.kioschool.kioschoolapi.global.common.enums.WebsocketType

data class OrderWebsocketEvent(
    val orderId: Long,
    val type: WebsocketType
)