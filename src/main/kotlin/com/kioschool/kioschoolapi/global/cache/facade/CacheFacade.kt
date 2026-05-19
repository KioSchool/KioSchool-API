package com.kioschool.kioschoolapi.global.cache.facade

import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.cache.service.CacheService
import org.springframework.stereotype.Component

@Component
class CacheFacade(
    private val cacheService: CacheService
) {
    fun getCacheNames(): List<String> = cacheService.getCacheNames()

    fun getCacheKeys(cacheName: String): List<String> {
        require(CacheNames.ALL.contains(cacheName)) { "Cache '$cacheName' not found" }
        return cacheService.getCacheKeys(cacheName)
    }

    fun clearAllCaches(): List<String> = cacheService.clearAllCaches()

    fun clearCache(cacheName: String): String {
        require(CacheNames.ALL.contains(cacheName)) { "Cache '$cacheName' not found" }
        cacheService.clearCache(cacheName)
        return cacheName
    }

    fun deleteCacheKey(cacheName: String, key: String): String {
        require(CacheNames.ALL.contains(cacheName)) { "Cache '$cacheName' not found" }
        cacheService.deleteCacheKey(cacheName, key)
        return key
    }
}
