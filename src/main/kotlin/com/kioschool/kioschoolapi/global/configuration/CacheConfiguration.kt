package com.kioschool.kioschoolapi.global.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheWriter
import java.time.Duration

@EnableCaching
@Configuration
class CacheConfiguration {

    class CustomRedisCacheManager(
        cacheWriter: RedisCacheWriter,
        defaultCacheConfiguration: RedisCacheConfiguration,
        initialCacheConfigurations: Map<String, RedisCacheConfiguration>
    ) : RedisCacheManager(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations) {

        override fun createRedisCache(name: String, cacheConfig: RedisCacheConfiguration?): RedisCache {
            var finalName = name
            var finalConfig = cacheConfig ?: defaultCacheConfiguration
            
            if (name.contains("#")) {
                val parts = name.split("#")
                if (parts.size == 2) {
                    finalName = parts[0]
                    val ttlDuration = parseDuration(parts[1])
                    if (ttlDuration != null) {
                        finalConfig = finalConfig.entryTtl(ttlDuration)
                    }
                }
            }
            return super.createRedisCache(finalName, finalConfig)
        }

        private fun parseDuration(ttlString: String): Duration? {
            return when {
                ttlString.endsWith("s", ignoreCase = true) -> {
                    ttlString.dropLast(1).toLongOrNull()?.let { Duration.ofSeconds(it) }
                }
                ttlString.endsWith("m", ignoreCase = true) -> {
                    ttlString.dropLast(1).toLongOrNull()?.let { Duration.ofMinutes(it) }
                }
                ttlString.endsWith("h", ignoreCase = true) -> {
                    ttlString.dropLast(1).toLongOrNull()?.let { Duration.ofHours(it) }
                }
                else -> {
                    ttlString.toLongOrNull()?.let { Duration.ofMinutes(it) } // 기본 분 단위
                }
            }
        }
    }

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Any::class.java)
                    .build(),
                ObjectMapper.DefaultTyping.EVERYTHING
            )
        }

        val redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer(objectMapper)
                )
            )
            .entryTtl(Duration.ofMinutes(30))

        val cacheConfigurations = CacheNames.ALL.associateWith { redisCacheConfiguration }

        val cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory)
        return CustomRedisCacheManager(cacheWriter, redisCacheConfiguration, cacheConfigurations)
    }
}