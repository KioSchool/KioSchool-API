package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class InquiryEmailGuardTest : DescribeSpec({
    val valueOperations = mockk<ValueOperations<String, Int>>()
    val redisTemplate = mockk<RedisTemplate<String, Int>>()
    val sut = InquiryEmailGuard(valueOperations, redisTemplate, maxCount = 3, windowMinutes = 60L)

    afterTest { clearAllMocks() }

    describe("check") {
        it("첫 요청이면 TTL을 건다") {
            every { valueOperations.increment("inquiry:guard:email:a@b.com", 1) } returns 1L
            every { redisTemplate.getExpire("inquiry:guard:email:a@b.com") } returns -2L
            every { redisTemplate.expire("inquiry:guard:email:a@b.com", Duration.ofMinutes(60)) } returns true

            shouldNotThrowAny { sut.check("a@b.com") }

            verify(exactly = 1) {
                redisTemplate.expire("inquiry:guard:email:a@b.com", Duration.ofMinutes(60))
            }
        }

        it("두 번째 요청부터는 TTL을 다시 걸지 않는다") {
            every { valueOperations.increment("inquiry:guard:email:a@b.com", 1) } returns 2L
            every { redisTemplate.getExpire("inquiry:guard:email:a@b.com") } returns 100L

            shouldNotThrowAny { sut.check("a@b.com") }

            verify(exactly = 0) { redisTemplate.expire(any(), any<Duration>()) }
        }

        it("정확히 상한이면 통과한다") {
            every { valueOperations.increment("inquiry:guard:email:a@b.com", 1) } returns 3L
            every { redisTemplate.getExpire("inquiry:guard:email:a@b.com") } returns 100L

            shouldNotThrowAny { sut.check("a@b.com") }
        }

        it("상한을 넘으면 INQUIRY_RATE_LIMIT_EXCEEDED") {
            every { valueOperations.increment("inquiry:guard:email:a@b.com", 1) } returns 4L
            every { redisTemplate.getExpire("inquiry:guard:email:a@b.com") } returns 100L

            val ex = shouldThrow<CustomException> { sut.check("a@b.com") }
            ex.errorCode shouldBe ErrorCode.INQUIRY_RATE_LIMIT_EXCEEDED
        }

        it("Redis가 죽어 있으면 통과시킨다 (fail-open)") {
            every { valueOperations.increment(any(), any<Long>()) } throws
                RedisConnectionFailureException("connection refused")

            shouldNotThrowAny { sut.check("a@b.com") }
        }

        it("increment가 null이면 통과시킨다") {
            every { valueOperations.increment(any(), any<Long>()) } returns null

            shouldNotThrowAny { sut.check("a@b.com") }
        }
    }
})
