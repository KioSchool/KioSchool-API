package com.kioschool.kioschoolapi.global.websocket.redis

import com.kioschool.kioschoolapi.global.websocket.dto.Message
import com.kioschool.kioschoolapi.global.websocket.dto.RedisPubSubMessage
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.stereotype.Service

@Service
class RedisPublisher(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val channelTopic: ChannelTopic
) {
    fun publish(destination: String, message: Message) {
        val pubSubMessage = RedisPubSubMessage(destination, message)
        redisTemplate.convertAndSend(channelTopic.topic, pubSubMessage)
    }
}
