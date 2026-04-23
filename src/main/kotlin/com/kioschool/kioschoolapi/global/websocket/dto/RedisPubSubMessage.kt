package com.kioschool.kioschoolapi.global.websocket.dto

data class RedisPubSubMessage(
    val destination: String,
    val payload: Message
)
