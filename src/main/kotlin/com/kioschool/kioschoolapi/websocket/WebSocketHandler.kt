package com.kioschool.kioschoolapi.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class WebSocketHandler : TextWebSocketHandler() {
    private val sessions = HashMap<Long, HashMap<String, WebSocketSession>>()
    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.attributes["workspaceId"] as Long] = HashMap()
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        // todo()
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions[session.attributes["workspaceId"] as Long]?.remove(session.attributes["username"] as String)
    }
}