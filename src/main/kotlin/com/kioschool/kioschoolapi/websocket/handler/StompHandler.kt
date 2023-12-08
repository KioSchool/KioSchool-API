package com.kioschool.kioschoolapi.websocket.handler

import com.kioschool.kioschoolapi.security.JwtProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

@Component
class StompHandler(
    private val jwtProvider: JwtProvider,
) : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command == StompCommand.SUBSCRIBE) {
            val token = accessor.getFirstNativeHeader("Authorization") ?: return null
            if (!jwtProvider.validateToken(token)) throw Exception("Invalid token")
        }

        return message
    }
}