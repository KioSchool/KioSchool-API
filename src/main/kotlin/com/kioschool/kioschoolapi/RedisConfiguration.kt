package com.kioschool.kioschoolapi

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

@Configuration
class RedisConfiguration(
    @Value("\${spring.data.redis.host}")
    private val host: String,
    @Value("\${spring.data.redis.port}")
    private val port: Int,
    @Value("\${spring.data.redis.password}")
    private val password: String
) {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val factory = LettuceConnectionFactory(host, port)
        factory.setPassword(password)
        return factory
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Int> {
        val redisTemplate = RedisTemplate<String, Int>()

        redisTemplate.connectionFactory = redisConnectionFactory()
        redisTemplate.keySerializer = redisTemplate.stringSerializer

        return redisTemplate
    }

    @Bean
    fun getValueOperations(): ValueOperations<String, Int> {
        return redisTemplate().opsForValue()
    }
}