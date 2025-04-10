package com.kioschool.kioschoolapi.websocket.dto

import com.kioschool.kioschoolapi.common.enums.WebsocketType

data class Message(
    val type: WebsocketType,
    val data: Any
)