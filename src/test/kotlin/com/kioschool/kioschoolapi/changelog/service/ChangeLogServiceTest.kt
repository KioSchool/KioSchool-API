package com.kioschool.kioschoolapi.changelog.service

import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeLog
import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeTargetType
import com.kioschool.kioschoolapi.domain.changelog.repository.ChangeLogRepository
import com.kioschool.kioschoolapi.domain.changelog.service.ChangeLogService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class ChangeLogServiceTest : DescribeSpec({
    val changeLogRepository = mockk<ChangeLogRepository>()

    val sut = ChangeLogService(changeLogRepository)

    afterTest {
        clearAllMocks()
    }

    describe("record") {
        it("should save a row with the given values when old and new differ") {
            val saved = slot<ChangeLog>()
            every { changeLogRepository.save(capture(saved)) } answers { saved.captured }

            sut.record(1L, ChangeTargetType.ORDER, 10L, "status", "PAID", "CANCELLED")

            saved.captured.workspaceId shouldBe 1L
            saved.captured.targetType shouldBe ChangeTargetType.ORDER
            saved.captured.targetId shouldBe 10L
            saved.captured.field shouldBe "status"
            saved.captured.oldValue shouldBe "PAID"
            saved.captured.newValue shouldBe "CANCELLED"
        }

        it("should save when only one side is null") {
            every { changeLogRepository.save(any()) } answers { firstArg() }

            sut.record(1L, ChangeTargetType.ORDER_SESSION, 10L, "expected_end_at", null, "2026-10-05T22:00:00")

            verify(exactly = 1) { changeLogRepository.save(any()) }
        }

        it("should not save when old and new are equal") {
            sut.record(1L, ChangeTargetType.ORDER, 10L, "status", "PAID", "PAID")

            verify(exactly = 0) { changeLogRepository.save(any()) }
        }

        it("should not save when both are null") {
            sut.record(1L, ChangeTargetType.ORDER_SESSION, 10L, "expected_end_at", null, null)

            verify(exactly = 0) { changeLogRepository.save(any()) }
        }
    }
})
