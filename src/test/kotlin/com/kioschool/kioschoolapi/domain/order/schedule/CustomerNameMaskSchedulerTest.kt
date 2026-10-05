package com.kioschool.kioschoolapi.domain.order.schedule

import com.kioschool.kioschoolapi.domain.order.dto.common.MaskedCustomerNameCount
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.mockk.*
import java.time.LocalDateTime

class CustomerNameMaskSchedulerTest : DescribeSpec({
    val orderService = mockk<OrderService>()
    val sut = CustomerNameMaskScheduler(orderService, retentionDays = 90)

    afterTest { clearAllMocks() }

    describe("maskExpiredCustomerNames") {
        it("보관 기간(90일) 전을 기준 시각으로 가린다") {
            val cutoff = slot<LocalDateTime>()
            every { orderService.maskCustomerNamesCreatedBefore(capture(cutoff)) } returns MaskedCustomerNameCount(1, 1)

            val before = LocalDateTime.now().minusDays(90)
            sut.maskExpiredCustomerNames()
            val after = LocalDateTime.now().minusDays(90)

            cutoff.captured shouldBeAfter before.minusNanos(1)
            cutoff.captured shouldBeBefore after.plusNanos(1)
        }

        it("실패해도 예외를 던지지 않는다 (서버 기동을 막지 않는다)") {
            every { orderService.maskCustomerNamesCreatedBefore(any()) } throws RuntimeException("db down")

            shouldNotThrowAny { sut.maskOnStartup() }
        }
    }
})
