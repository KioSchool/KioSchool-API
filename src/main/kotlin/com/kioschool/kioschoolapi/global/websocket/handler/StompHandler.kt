package com.kioschool.kioschoolapi.global.websocket.handler

import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.security.JwtProvider
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
                if (!isValidToken(token)) throw CustomException(ErrorCode.INVALID_JWT)
            }

            StompCommand.SUBSCRIBE -> {
                val token = sessionAttributes?.get("token") as String
                if (!isValidToken(token)) throw CustomException(ErrorCode.INVALID_JWT)
                if (!isAccessible(token, accessor)) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
            }

            // 주문 채널 메시지는 서버만 보낸다. 클라이언트는 구독만 한다.
            StompCommand.SEND -> throw CustomException(ErrorCode.ACCESS_DENIED)

            else -> {}
        }

        return message
    }

    // 만료된 access 쿠키는 브라우저가 보내지 않으므로 빈 토큰은 흔하다. 파싱 경고 로그를 남기지 않고 거부한다.
    private fun isValidToken(token: String) = token.isNotBlank() && jwtProvider.isValidToken(token)

    fun isAccessible(token: String, accessor: StompHeaderAccessor): Boolean {
        val username = jwtProvider.getLoginId(token)
        // 구독 경로는 /sub/order/{주점 id} 형식만 받는다.
        val workspaceId = ORDER_DESTINATION.matchEntire(accessor.destination ?: return false)
            ?.groupValues?.get(1)?.toLongOrNull() ?: return false
        return workspaceService.isAccessible(username, workspaceId)
    }

    companion object {
        private val ORDER_DESTINATION = Regex("/sub/order/(\\d+)")
    }
}
