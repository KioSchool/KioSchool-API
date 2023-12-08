package com.kioschool.kioschoolapi.websocket.service

import com.kioschool.kioschoolapi.websocket.dto.Message
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class WebsocketService(
    private val simpMessagingTemplate: SimpMessagingTemplate
) {
    fun sendMessage(destination: String, message: Message) {
        simpMessagingTemplate.convertAndSend(destination, message)
    }
}