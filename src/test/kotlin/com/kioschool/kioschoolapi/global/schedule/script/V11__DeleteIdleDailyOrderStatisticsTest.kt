package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

class V11__DeleteIdleDailyOrderStatisticsTest : DescribeSpec({
    val repository = mockk<DailyOrderStatisticRepository>()
    val sut = V11__DeleteIdleDailyOrderStatistics(repository)

    fun mockStat(id: Long, idle: Boolean): DailyOrderStatistic {
        val stat = mockk<DailyOrderStatistic>()
        every { stat.id } returns id
        every { stat.isIdleDay() } returns idle
        return stat
    }

    beforeTest { every { repository.deleteAllByIdInBatch(any()) } just Runs }
    afterTest { clearAllMocks() }

    describe("run") {
        it("0건 통계 중 전일에도 주문이 없던 row만 지운다") {
            every { repository.findAllByTotalOrders(0) } returns listOf(
                mockStat(id = 1L, idle = true),
                mockStat(id = 2L, idle = false),
                mockStat(id = 3L, idle = true)
            )

            sut.run()

            verify(exactly = 1) { repository.deleteAllByIdInBatch(listOf(1L, 3L)) }
        }

        it("대상이 많으면 1000개씩 나눠 지운다") {
            every { repository.findAllByTotalOrders(0) } returns (1L..2500L).map { mockStat(id = it, idle = true) }

            sut.run()

            verify(exactly = 1) { repository.deleteAllByIdInBatch((1L..1000L).toList()) }
            verify(exactly = 1) { repository.deleteAllByIdInBatch((1001L..2000L).toList()) }
            verify(exactly = 1) { repository.deleteAllByIdInBatch((2001L..2500L).toList()) }
        }

        it("대상이 없으면 지우지 않는다") {
            every { repository.findAllByTotalOrders(0) } returns emptyList()

            sut.run()

            verify(exactly = 0) { repository.deleteAllByIdInBatch(any()) }
        }
    }
})
