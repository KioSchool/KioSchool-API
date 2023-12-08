package com.kioschool.kioschoolapi.websocket.dto

data class Message(
    val type: String,
    val data: Any
)