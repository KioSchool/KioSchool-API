package com.kioschool.kioschoolapi.global.cache.handler

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.mockk
import org.springframework.cache.Cache

class RedisCacheErrorHandlerTest : DescribeSpec({
    val sut = RedisCacheErrorHandler()
    val cache = mockk<Cache>(relaxed = true)
    val boom = RuntimeException("Connection reset")

    describe("RedisCacheErrorHandler") {
        // 핵심 계약: 캐시 연산 실패를 재던지지 않고 삼켜야 @Cacheable 메서드가 원본(DB)으로 폴백한다.
        it("swallows GET errors so the request falls back to the source") {
            shouldNotThrowAny { sut.handleCacheGetError(boom, cache, "key") }
        }

        it("swallows PUT errors") {
            shouldNotThrowAny { sut.handleCachePutError(boom, cache, "key", "value") }
        }

        it("swallows EVICT errors") {
            shouldNotThrowAny { sut.handleCacheEvictError(boom, cache, "key") }
        }

        it("swallows CLEAR errors") {
            shouldNotThrowAny { sut.handleCacheClearError(boom, cache) }
        }
    }
})
