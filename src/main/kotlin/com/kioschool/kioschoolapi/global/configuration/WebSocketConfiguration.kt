package com.kioschool.kioschoolapi.global.configuration

import com.kioschool.kioschoolapi.global.websocket.handler.StompHandler
import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.util.WebUtils

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration(
    private val stompHandler: StompHandler,
    @Value("\${websocket.allowed-origins}")
    private val allowedOrigins: String,
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins)
            .addInterceptors(stompHandshakeInterceptor())
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompHandler)
    }

    override fun configureMessageConverters(messageConverters: MutableList<MessageConverter>): Boolean {
        return true
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/sub")
    }

    @Bean
    fun stompHandshakeInterceptor(): HandshakeInterceptor {
        return object : HandshakeInterceptor {
            override fun beforeHandshake(
                request: ServerHttpRequest,
                response: ServerHttpResponse,
                wsHandler: WebSocketHandler,
                attributes: MutableMap<String, Any>
            ): Boolean {
                if (request is ServletServerHttpRequest) {
                    val servletRequest = request.servletRequest
                    val token: Cookie? =
                        WebUtils.getCookie(servletRequest, HttpHeaders.AUTHORIZATION)
                    attributes["token"] = token?.value ?: ""
                }
                return true
            }

            override fun afterHandshake(
                request: ServerHttpRequest,
                response: ServerHttpResponse,
                wsHandler: WebSocketHandler,
                exception: Exception?
            ) {
            }
        }
    }
}