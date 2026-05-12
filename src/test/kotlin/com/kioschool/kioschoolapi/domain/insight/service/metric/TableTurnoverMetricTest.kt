package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class TableTurnoverMetricTest : DescribeSpec({
    val sut = TableTurnoverMetric()

    fun mockStat(tableTurnoverRate: Double): DailyOrderStatistic {
        val stat = mockk<DailyOrderStatistic>()
        every { stat.tableTurnoverRate } returns tableTurnoverRate
        return stat
    }

    describe("evaluate") {
        it("returns percentile relative to cohort peers") {
            val self = mockStat(tableTurnoverRate = 3.0)
            val peers = listOf(1.0, 2.0, 2.5, 3.0, 4.0).map { mockStat(it) }
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result!!.metricKey shouldBe "table-turnover"
            result.absoluteValue shouldBe 3.0
            // sorted peers: [1.0, 2.0, 2.5, 3.0, 4.0]
            // values < 3.0 → 3 (1.0, 2.0, 2.5) → 3 / 5 = 0.6 → 60%
            result.percentile shouldBe 60.0
            // cohort avg = (1.0+2.0+2.5+3.0+4.0)/5 = 2.5
            // ratio = (3.0 - 2.5) / 2.5 = 0.2
            result.cohortAverageRatio!! shouldBe (0.5 / 2.5)
        }

        it("returns null when stat has no turnover (tableTurnoverRate = 0)") {
            val self = mockStat(tableTurnoverRate = 0.0)
            val peers = listOf(1.0, 2.0, 2.5, 3.0, 4.0).map { mockStat(it) }
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }

        it("returns null when cohort has fewer than 2 peers") {
            val self = mockStat(tableTurnoverRate = 3.0)
            val peers = listOf(mockStat(2.0))
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }
    }
})
