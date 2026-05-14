package com.kioschool.kioschoolapi.domain.insight.service.metric.milestone

import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.TableCountBucket
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class RevenueMilestoneMetricTest : DescribeSpec({
    val properties = InsightProperties().apply {
        milestone = InsightProperties.Milestone().apply {
            revenueStepSize = 1_000_000L
        }
    }
    val sut = RevenueMilestoneMetric(properties)

    fun mockStat(totalRevenue: Long): DailyOrderStatistic {
        val stat = mockk<DailyOrderStatistic>()
        every { stat.totalRevenue } returns totalRevenue
        return stat
    }

    val cohort = CohortContext(bucket = TableCountBucket.S, peers = emptyList())

    describe("evaluate") {
        it("returns floor-rounded step for revenue of 6_500_000") {
            val self = mockStat(totalRevenue = 6_500_000L)

            val result = sut.evaluate(self, cohort)

            result!!.metricKey shouldBe "revenue-milestone"
            result.absoluteValue shouldBe 6_500_000L
            result.milestoneStep shouldBe 6_000_000L
            result.percentile.shouldBeNull()
            result.cohortAverageRatio.shouldBeNull()
        }

        it("returns exact step when revenue is exactly on a boundary") {
            val self = mockStat(totalRevenue = 2_000_000L)

            val result = sut.evaluate(self, cohort)

            result!!.milestoneStep shouldBe 2_000_000L
        }

        it("returns null when revenue is below first step") {
            val self = mockStat(totalRevenue = 500_000L)

            val result = sut.evaluate(self, cohort)

            result.shouldBeNull()
        }
    }
})
