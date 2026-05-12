package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class AverageOrderAmountMetricTest : DescribeSpec({
    val sut = AverageOrderAmountMetric()

    fun mockStat(averageOrderAmount: Int): DailyOrderStatistic {
        val stat = mockk<DailyOrderStatistic>()
        every { stat.averageOrderAmount } returns averageOrderAmount
        return stat
    }

    describe("evaluate") {
        it("returns percentile relative to cohort peers") {
            val self = mockStat(averageOrderAmount = 14000)
            val peers = listOf(8000, 9000, 11000, 14000, 18000).map { mockStat(it) }
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result!!.metricKey shouldBe "average-order-amount"
            result.absoluteValue shouldBe 14000
            // sorted peers: [8000, 9000, 11000, 14000, 18000]
            // values < 14000 → 3 (8000, 9000, 11000) → 3 / 5 = 0.6 → 60%
            result.percentile shouldBe 60.0
            // cohort avg = (8000+9000+11000+14000+18000)/5 = 12000
            // ratio = (14000 - 12000) / 12000 = 0.1666...
            result.cohortAverageRatio!! shouldBe (2000.0 / 12000.0)
        }

        it("returns null when stat has no orders (averageOrderAmount = 0)") {
            val self = mockStat(averageOrderAmount = 0)
            val peers = listOf(8000, 9000, 11000, 14000, 18000).map { mockStat(it) }
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }

        it("returns null when cohort has fewer than 2 peers") {
            val self = mockStat(averageOrderAmount = 14000)
            val peers = listOf(mockStat(10000))
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }
    }
})
