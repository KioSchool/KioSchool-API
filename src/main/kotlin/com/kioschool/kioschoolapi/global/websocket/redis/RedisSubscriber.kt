package com.kioschool.kioschoolapi.global.websocket.redis

import com.kioschool.kioschoolapi.global.websocket.dto.RedisPubSubMessage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class RedisSubscriber(
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisMessageListenerContainer: RedisMessageListenerContainer,
    private val channelTopic: ChannelTopic,
    @Qualifier("redisPubSubTemplate")
    private val redisTemplate: RedisTemplate<String, Any>
) : MessageListener {

    init {
        redisMessageListenerContainer.addMessageListener(this, channelTopic)
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val pubSubMessage =
                redisTemplate.valueSerializer.deserialize(message.body) as RedisPubSubMessage
            messagingTemplate.convertAndSend(pubSubMessage.destination, pubSubMessage.payload)
        } catch (e: Exception) {
            println("Redis Subscriber Error: ${e.message}")
        }
    }
}
