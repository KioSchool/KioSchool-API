package com.kioschool.kioschoolapi.global.websocket.service

import com.kioschool.kioschoolapi.global.websocket.dto.Message
import com.kioschool.kioschoolapi.global.websocket.redis.RedisPublisher
import org.springframework.stereotype.Service

@Service
class CustomWebSocketService(
    private val redisPublisher: RedisPublisher
) {
    fun sendMessage(destination: String, message: Message) {
        redisPublisher.publish(destination, message)
    }
}