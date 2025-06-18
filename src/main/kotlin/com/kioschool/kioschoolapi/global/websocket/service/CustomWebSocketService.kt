package com.kioschool.kioschoolapi.global.websocket.service

import com.kioschool.kioschoolapi.global.websocket.dto.Message
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class CustomWebSocketService(
    private val simpMessagingTemplate: SimpMessagingTemplate
) {
    fun sendMessage(destination: String, message: Message) {
        simpMessagingTemplate.convertAndSend(destination, message)
    }
}