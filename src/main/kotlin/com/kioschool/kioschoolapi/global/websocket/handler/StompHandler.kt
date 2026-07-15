package com.kioschool.kioschoolapi.global.websocket.handler

import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.security.JwtProvider
import com.kioschool.kioschoolapi.global.security.exception.InvalidJwtException
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

@Component
class StompHandler(
    private val jwtProvider: JwtProvider,
    private val workspaceService: WorkspaceService
) : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        val sessionAttributes = accessor.sessionAttributes
        when (accessor.command) {
            StompCommand.CONNECT -> {
                val token = sessionAttributes?.get("token") as String
                if (!jwtProvider.isValidToken(token)) throw InvalidJwtException()
            }

            StompCommand.SUBSCRIBE -> {
                val token = sessionAttributes?.get("token") as String
                if (!jwtProvider.isValidToken(token)) throw InvalidJwtException()
                if (!isAccessible(token, accessor)) throw WorkspaceInaccessibleException()
            }

            else -> {}
        }

        return message
    }

    fun isAccessible(token: String, accessor: StompHeaderAccessor): Boolean {
        val username = jwtProvider.getLoginId(token)
        val workspaceId = accessor.destination!!.filter { it.isDigit() }.toLong()
        return workspaceService.isAccessible(username, workspaceId)
    }
}