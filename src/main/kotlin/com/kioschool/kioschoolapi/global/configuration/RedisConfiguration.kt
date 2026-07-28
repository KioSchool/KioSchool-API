package com.kioschool.kioschoolapi.global.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule
import java.time.Duration

@Configuration
class RedisConfiguration(
    @Value("\${spring.data.redis.host}")
    private val host: String,
    @Value("\${spring.data.redis.port}")
    private val port: Int,
    @Value("\${spring.data.redis.password}")
    private val password: String,
    // yml의 spring.data.redis.ssl.enabled는 커넥션 팩토리를 수동 빈으로 만들면
    // Boot 자동설정이 backoff 되어 무시된다. 여기서 직접 읽어 적용한다. (local은 미설정=false)
    @Value("\${spring.data.redis.ssl.enabled:false}")
    private val sslEnabled: Boolean
) {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val configuration = RedisStandaloneConfiguration(host, port)
        configuration.setPassword(password)

        // TCP keep-alive로 GCP 네트워크 장비/Redis idle-timeout에 의한 유휴 커넥션의 조용한 reap을
        // 방지하고, 끊긴 커넥션은 command timeout으로 빠르게 실패시켜 CacheErrorHandler가 DB로
        // 폴백하도록 한다. (기본값은 keep-alive 없음 + 60s command timeout이라 Connection reset이
        // 그대로 요청 실패로 이어졌음)
        val socketOptions = SocketOptions.builder()
            .keepAlive(
                SocketOptions.KeepAliveOptions.builder()
                    .enable()
                    .idle(Duration.ofSeconds(60))
                    .interval(Duration.ofSeconds(10))
                    .count(3)
                    .build()
            )
            .connectTimeout(Duration.ofSeconds(5))
            .build()

        val clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .autoReconnect(true)
            .build()

        val clientConfigurationBuilder = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofSeconds(3))
        if (sslEnabled) clientConfigurationBuilder.useSsl()

        return LettuceConnectionFactory(configuration, clientConfigurationBuilder.build())
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Int> {
        val redisTemplate = RedisTemplate<String, Int>()

        redisTemplate.connectionFactory = redisConnectionFactory()
        redisTemplate.keySerializer = redisTemplate.stringSerializer

        return redisTemplate
    }

    @Bean
    fun redisPubSubTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper())
        return template
    }

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        return container
    }

    @Bean
    fun internalTopic(): ChannelTopic {
        return ChannelTopic("websocket-topic")
    }

    @Bean
    fun getValueOperations(): ValueOperations<String, Int> {
        return redisTemplate().opsForValue()
    }



    private fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(Hibernate5JakartaModule())
            registerModule(KotlinModule.Builder().build())
            activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Any::class.java)
                    .build(),
                ObjectMapper.DefaultTyping.EVERYTHING
            )
        }
    }
}