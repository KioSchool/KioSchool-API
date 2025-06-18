package com.kioschool.kioschoolapi.global.websocket.dto

import com.kioschool.kioschoolapi.global.common.enums.WebsocketType

data class Message(
    val type: WebsocketType,
    val data: Any
)