package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 같은 이메일로 반복 접수하는 것을 막는다.
 *
 * 접수 확인 메일 때문에 POST /inquiries가 임의 주소로 메일을 보내는 공개 트리거가 되므로,
 * 전면 rate limit 대신 이 벡터만 막는다.
 *
 * Redis 예외는 삼키고 통과시킨다(fail-open). 과거 Redis 장애로 프로덕션 502가 난 적이 있어,
 * 이 가드가 문의 접수의 단일 실패 지점이 되어서는 안 된다.
 */
@Component
class InquiryEmailGuard(
    private val valueOperations: ValueOperations<String, Int>,
    private val redisTemplate: RedisTemplate<String, Int>,
    @Value("\${inquiry.guard.email.max-count}")
    private val maxCount: Int,
    @Value("\${inquiry.guard.email.window-minutes}")
    private val windowMinutes: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun check(normalizedEmail: String) {
        val key = "$KEY_PREFIX$normalizedEmail"

        val count = try {
            valueOperations.increment(key, 1)
        } catch (e: Exception) {
            log.warn("Inquiry email guard skipped (redis unavailable): {}", e.message)
            return
        } ?: return

        if (count == 1L) {
            runCatching { redisTemplate.expire(key, Duration.ofMinutes(windowMinutes)) }
                .onFailure { log.warn("Failed to set TTL on {}: {}", key, it.message) }
        }

        if (count > maxCount) throw CustomException(ErrorCode.INQUIRY_RATE_LIMIT_EXCEEDED)
    }

    companion object {
        private const val KEY_PREFIX = "inquiry:guard:email:"
    }
}
