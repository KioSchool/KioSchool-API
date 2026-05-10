package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.dto.SalesByHour
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PeakHourRevenueMetricTest : DescribeSpec({
    val sut = PeakHourRevenueMetric()

    fun mockStat(salesByHour: List<SalesByHour>): DailyOrderStatistic {
        val stat = mockk<DailyOrderStatistic>()
        every { stat.salesByHour } returns salesByHour
        return stat
    }

    fun salesAt(hour: Int, revenue: Long): SalesByHour =
        SalesByHour(hour = hour, orderCount = 1, revenue = revenue)

    describe("evaluate") {
        it("returns percentile of peak revenue and stores peakHour in milestoneStep") {
            val self = mockStat(
                listOf(
                    salesAt(11, 30_000L),
                    salesAt(19, 100_000L),
                    salesAt(20, 50_000L)
                )
            )
            val peers = listOf(40_000L, 60_000L, 80_000L, 100_000L, 120_000L).map {
                mockStat(listOf(salesAt(18, it)))
            }
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result!!.metricKey shouldBe "peak-hour-revenue"
            result.absoluteValue shouldBe 100_000L
            // sorted peer peaks: [40000, 60000, 80000, 100000, 120000]
            // values < 100000 → 3 (40000, 60000, 80000) → 3 / 5 = 0.6 → 60%
            result.percentile shouldBe 60.0
            // cohort avg = (40000+60000+80000+100000+120000)/5 = 80000
            // ratio = (100000 - 80000) / 80000 = 0.25
            result.cohortAverageRatio!! shouldBe (20_000.0 / 80_000.0)
            // peakHour stored in milestoneStep
            result.milestoneStep shouldBe 19L
        }

        it("returns null when stat has no sales (peak = 0)") {
            val self = mockStat(emptyList())
            val peers = listOf(40_000L, 60_000L, 80_000L, 100_000L, 120_000L).map {
                mockStat(listOf(salesAt(18, it)))
            }
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }

        it("returns null when cohort has fewer than 2 peers") {
            val self = mockStat(listOf(salesAt(19, 100_000L)))
            val peers = listOf(mockStat(listOf(salesAt(18, 60_000L))))
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }
    }
})
