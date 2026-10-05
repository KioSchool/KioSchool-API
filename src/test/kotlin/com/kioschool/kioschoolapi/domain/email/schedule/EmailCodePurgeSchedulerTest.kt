package com.kioschool.kioschoolapi.domain.email.schedule

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.mockk.*
import java.time.LocalDateTime

class EmailCodePurgeSchedulerTest : DescribeSpec({
    val emailService = mockk<EmailService>()
    val sut = EmailCodePurgeScheduler(emailService, retentionDays = 7)

    afterTest { clearAllMocks() }

    describe("purgeExpiredCodes") {
        it("보관 기간(7일) 전에 마지막으로 갱신된 코드를 지운다") {
            val cutoff = slot<LocalDateTime>()
            every { emailService.deleteCodesUpdatedBefore(capture(cutoff)) } returns 2

            val before = LocalDateTime.now().minusDays(7)
            sut.purgeExpiredCodes()
            val after = LocalDateTime.now().minusDays(7)

            cutoff.captured shouldBeAfter before.minusNanos(1)
            cutoff.captured shouldBeBefore after.plusNanos(1)
        }

        it("실패해도 예외를 던지지 않는다 (서버 기동을 막지 않는다)") {
            every { emailService.deleteCodesUpdatedBefore(any()) } throws RuntimeException("db down")

            shouldNotThrowAny { sut.purgeOnStartup() }
        }
    }
})
