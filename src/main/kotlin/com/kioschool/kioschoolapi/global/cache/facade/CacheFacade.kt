package com.kioschool.kioschoolapi.global.cache.facade

import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.cache.service.CacheService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CacheFacade(
    private val cacheService: CacheService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getCacheNames(): List<String> = cacheService.getCacheNames()

    fun getCacheKeys(cacheName: String): List<String> {
        require(CacheNames.ALL.contains(cacheName)) { "Cache '$cacheName' not found" }
        return cacheService.getCacheKeys(cacheName)
    }

    fun clearAllCaches(): List<String> {
        val cleared = cacheService.clearAllCaches()
        log.info("[AUDIT] action=CLEAR_ALL_CACHES caches={}", cleared)
        return cleared
    }

    fun clearCache(cacheName: String): String {
        require(CacheNames.ALL.contains(cacheName)) { "Cache '$cacheName' not found" }
        cacheService.clearCache(cacheName)
        log.info("[AUDIT] action=CLEAR_CACHE cacheName={}", cacheName)
        return cacheName
    }

    fun deleteCacheKey(cacheName: String, key: String): String {
        require(CacheNames.ALL.contains(cacheName)) { "Cache '$cacheName' not found" }
        cacheService.deleteCacheKey(cacheName, key)
        log.info("[AUDIT] action=DELETE_CACHE_KEY cacheName={} key={}", cacheName, key)
        return key
    }
}
