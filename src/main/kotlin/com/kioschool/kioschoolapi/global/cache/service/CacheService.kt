package com.kioschool.kioschoolapi.global.cache.service

import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Service

@Service
class CacheService(
    private val cacheManager: CacheManager,
    private val redisConnectionFactory: RedisConnectionFactory
) {
    fun getCacheNames(): List<String> = CacheNames.ALL

    fun getCacheKeys(cacheName: String): List<String> {
        val prefix = "$cacheName::"
        val keys = mutableListOf<String>()

        redisConnectionFactory.connection.use { conn ->
            conn.scan(ScanOptions.scanOptions().match("$prefix*").count(1000).build()).use { cursor ->
                cursor.forEach { keyBytes ->
                    keys.add(String(keyBytes).removePrefix(prefix))
                }
            }
        }

        return keys
    }

    fun clearAllCaches(): List<String> {
        CacheNames.ALL.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
        return CacheNames.ALL
    }

    fun clearCache(cacheName: String) {
        cacheManager.getCache(cacheName)?.clear()
    }

    fun deleteCacheKey(cacheName: String, key: String) {
        redisConnectionFactory.connection.use { conn ->
            conn.keyCommands().del("$cacheName::$key".toByteArray())
        }
    }
}
