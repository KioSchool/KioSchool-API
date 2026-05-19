package com.kioschool.kioschoolapi.global.cache.controller

import com.kioschool.kioschoolapi.global.cache.facade.CacheFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Cache Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminCacheController(
    private val cacheFacade: CacheFacade
) {
    @Operation(summary = "캐시 목록 조회", description = "현재 등록된 캐시 이름 목록을 조회합니다.")
    @GetMapping("/caches")
    fun getCacheNames(): List<String> = cacheFacade.getCacheNames()

    @Operation(summary = "캐시 키 목록 조회", description = "특정 캐시에 저장된 키 목록을 조회합니다.")
    @GetMapping("/cache/{cacheName}/keys")
    fun getCacheKeys(@PathVariable cacheName: String): List<String> = cacheFacade.getCacheKeys(cacheName)

    @Operation(summary = "전체 캐시 삭제", description = "모든 Redis 캐시를 삭제합니다.")
    @DeleteMapping("/cache")
    fun clearAllCaches(): List<String> = cacheFacade.clearAllCaches()

    @Operation(summary = "특정 캐시 삭제", description = "지정한 이름의 Redis 캐시를 삭제합니다.")
    @DeleteMapping("/cache/{cacheName}")
    fun clearCache(@PathVariable cacheName: String): String = cacheFacade.clearCache(cacheName)

    @Operation(summary = "특정 캐시 키 삭제", description = "지정한 캐시의 특정 키를 삭제합니다.")
    @DeleteMapping("/cache/{cacheName}/key")
    fun deleteCacheKey(@PathVariable cacheName: String, @RequestParam key: String): String =
        cacheFacade.deleteCacheKey(cacheName, key)
}
