package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProducts
import com.kioschool.kioschoolapi.domain.statistics.dto.PreviousDayComparison
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class V12__BackfillDailyOrderStatisticsTest : DescribeSpec({
    val orderRepository = mockk<OrderRepository>()
    val dailyOrderStatisticRepository = mockk<DailyOrderStatisticRepository>()
    val statisticsCalculator = mockk<StatisticsCalculator>()
    val entityManager = mockk<EntityManager>(relaxed = true)
    val sut = V12__BackfillDailyOrderStatistics(
        orderRepository,
        dailyOrderStatisticRepository,
        statisticsCalculator,
        entityManager
    )

    val day = LocalDate.of(2025, 5, 10)

    fun statistic(totalOrders: Int, previousDayComparison: PreviousDayComparison?) = DailyOrderStatistic(
        workspace = mockk(),
        referenceDate = day,
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

    fun orders(vararg rows: Pair<Long, LocalDateTime>) =
        every { orderRepository.findAllValidOrderWorkspaceIdAndCreatedAt() } returns
            rows.map { arrayOf<Any>(it.first, it.second) }

    val saved = mutableListOf<DailyOrderStatistic>()

    beforeTest {
        saved.clear()
        every { dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(any(), any()) } returns Optional.empty()
        every { dailyOrderStatisticRepository.save(capture(saved)) } answers { firstArg() }
        every { statisticsCalculator.calculate(any(), any()) } returns statistic(0, null)
    }
    afterTest { clearAllMocks() }

    describe("run") {
        it("새벽 9시 이전 주문은 전날 영업일로 계산하고, 주문일과 다음 날을 날짜순으로 계산한다") {
            orders(1L to day.atTime(20, 0), 1L to day.plusDays(1).atTime(2, 0))

            sut.run()

            verifyOrder {
                statisticsCalculator.calculate(1L, day)
                statisticsCalculator.calculate(1L, day.plusDays(1))
            }
            verify(exactly = 2) { statisticsCalculator.calculate(1L, any()) }
        }

        it("이미 통계가 있는 날은 다시 만들지 않는다") {
            orders(1L to day.atTime(20, 0))
            every { dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(1L, day) } returns
                Optional.of(statistic(20, null))

            sut.run()

            verify(exactly = 0) { statisticsCalculator.calculate(1L, day) }
        }

        it("주문이 15건 이상이면 달력에 포함하고, 미만이면 제외한다") {
            orders(1L to day.atTime(20, 0), 2L to day.atTime(20, 0))
            every { statisticsCalculator.calculate(1L, day) } returns statistic(20, null)
            every { statisticsCalculator.calculate(2L, day) } returns statistic(3, null)

            sut.run()

            saved.map { it.totalOrders to it.excludedFromCalendar } shouldBe listOf(20 to false, 3 to true)
        }

        it("당일·전일 모두 주문이 없는 날은 만들지 않고, 전일에 주문이 있던 0건 날은 만든다") {
            orders(1L to day.atTime(20, 0))
            every { statisticsCalculator.calculate(1L, day) } returns statistic(20, null)
            every { statisticsCalculator.calculate(1L, day.plusDays(1)) } returns
                statistic(0, PreviousDayComparison(-100.0, -20))

            sut.run()

            saved.map { it.totalOrders } shouldBe listOf(20, 0)
        }

        it("현재 영업일 이후는 계산하지 않는다") {
            orders(1L to LocalDateTime.now().plusHours(1))

            sut.run()

            verify(exactly = 0) { statisticsCalculator.calculate(any(), any()) }
        }

        it("한 워크스페이스가 실패해도 나머지는 계속 채운다") {
            orders(1L to day.atTime(20, 0), 2L to day.atTime(20, 0))
            every { statisticsCalculator.calculate(1L, any()) } throws IllegalArgumentException("Workspace not found")
            every { statisticsCalculator.calculate(2L, day) } returns statistic(20, null)

            sut.run()

            saved.map { it.totalOrders } shouldBe listOf(20)
        }
    }
})
