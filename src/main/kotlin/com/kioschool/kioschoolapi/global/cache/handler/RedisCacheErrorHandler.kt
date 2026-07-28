package com.kioschool.kioschoolapi.global.cache.handler

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

/**
 * Redis 캐시 연산 실패를 로그로만 남기고 삼켜서, 캐시 계층 장애(Connection reset 등)가
 * 요청을 500으로 만들지 않고 원본(주로 DB) 조회로 자연스럽게 폴백되도록 한다.
 *
 * evict/clear 실패 시에는 stale 엔트리가 남을 수 있으나, 모든 캐시에 TTL(기본 30분)이
 * 걸려 있어 최대 TTL 내에 자가 회복된다 — 캐시 정합성보다 가용성을 우선한 트레이드오프.
 */
class RedisCacheErrorHandler : CacheErrorHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
        log.warn("Redis cache GET failed (cache={}, key={}) — falling back to source", cache.name, key, exception)
    }

    override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
        log.warn("Redis cache PUT failed (cache={}, key={}) — skipping cache write", cache.name, key, exception)
    }

    override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
        log.warn("Redis cache EVICT failed (cache={}, key={}) — stale entry may persist until TTL", cache.name, key, exception)
    }

    override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
        log.warn("Redis cache CLEAR failed (cache={}) — stale entries may persist until TTL", cache.name, exception)
    }
}
