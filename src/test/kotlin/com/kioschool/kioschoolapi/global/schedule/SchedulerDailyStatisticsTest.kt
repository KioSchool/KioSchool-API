package com.kioschool.kioschoolapi.global.schedule

import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProducts
import com.kioschool.kioschoolapi.domain.statistics.dto.PreviousDayComparison
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDate
import java.util.Optional

class SchedulerDailyStatisticsTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val statisticsCalculator = mockk<StatisticsCalculator>()
    val dailyOrderStatisticRepository = mockk<DailyOrderStatisticRepository>()
    val sut = Scheduler(
        mockk(),
        mockk(),
        mockk(),
        workspaceRepository,
        statisticsCalculator,
        dailyOrderStatisticRepository,
        mockk()
    )

    val workspace = mockk<Workspace>()

    fun statistic(totalOrders: Int, previousDayComparison: PreviousDayComparison?) = DailyOrderStatistic(
        workspace = workspace,
        referenceDate = LocalDate.now().minusDays(1),
        totalSalesVolume = 0,
        totalRevenue = 0,
        averageOrderAmount = 0,
        totalOrders = totalOrders,
        averageOrdersPerTable = 0.0,
        tableTurnoverRate = 0.0,
        averageStayTimeMinutes = 0.0,
        previousDayComparison = previousDayComparison,
        salesByHour = emptyList(),
        popularProducts = PopularProducts(emptyList(), emptyList(), emptyList())
    )

    beforeTest {
        every { workspace.id } returns 1L
        every { workspaceRepository.findAll() } returns listOf(workspace)
        every { dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(1L, any()) } returns Optional.empty()
        every { dailyOrderStatisticRepository.save(any()) } answers { firstArg() }
    }
    afterTest { clearAllMocks() }

    describe("generateDailyStatistics") {
        it("당일 주문이 없고 전일 대비 비교값이 없으면 만들지 않는다") {
            every { statisticsCalculator.calculate(1L, any()) } returns statistic(0, null)

            sut.generateDailyStatistics()

            verify(exactly = 0) { dailyOrderStatisticRepository.save(any()) }
        }

        it("당일 주문이 없고 전일 대비 주문 차이가 0이면 만들지 않는다") {
            every { statisticsCalculator.calculate(1L, any()) } returns
                statistic(0, PreviousDayComparison(0.0, 0))

            sut.generateDailyStatistics()

            verify(exactly = 0) { dailyOrderStatisticRepository.save(any()) }
        }

        it("당일 주문이 없어도 전일엔 주문이 있었으면 달력 제외로 만든다") {
            val slot = slot<DailyOrderStatistic>()
            every { statisticsCalculator.calculate(1L, any()) } returns
                statistic(0, PreviousDayComparison(-100.0, -30))
            every { dailyOrderStatisticRepository.save(capture(slot)) } answers { firstArg() }

            sut.generateDailyStatistics()

            slot.captured.excludedFromCalendar shouldBe true
        }

        it("당일 주문이 있으면 만든다") {
            every { statisticsCalculator.calculate(1L, any()) } returns
                statistic(20, PreviousDayComparison(0.0, 20))

            sut.generateDailyStatistics()

            verify(exactly = 1) { dailyOrderStatisticRepository.save(any()) }
        }
    }
})
