package com.kioschool.kioschoolapi.websocket.handler

import com.kioschool.kioschoolapi.common.exception.InvalidJwtException
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
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
                if (!jwtProvider.validateToken(token)) throw InvalidJwtException()
            }

            StompCommand.SUBSCRIBE -> {
                val token = sessionAttributes?.get("token") as String
                if (!jwtProvider.validateToken(token)) throw InvalidJwtException()
                if (!isAccessible(token, accessor)) throw WorkspaceInaccessibleException()
            }

            else -> {}
        }

        return message
    }

    fun isAccessible(token: String, accessor: StompHeaderAccessor): Boolean {
        val username = jwtProvider.getLoginId(token)
        val workspace = workspaceService.getWorkspace(
            accessor.destination!!.filter { it.isDigit() }.toLong()
        )
        return workspaceService.isAccessible(username, workspace)
    }
}