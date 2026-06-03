package com.kioschool.kioschoolapi.global.websocket.dto

data class RedisPubSubMessage(
    val workspaceId: Long,
    val payload: Message
)
