package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.InsightMetric
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricCategory
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricResult
import com.kioschool.kioschoolapi.domain.insight.service.metric.TableCountBucket
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk

class InsightScoringServiceTest : DescribeSpec({
    val properties = InsightProperties().apply {
        thresholdPercentile = 80.0
    }

    fun mockMetric(
        key: String,
        category: MetricCategory,
        supports: Boolean = true,
        result: MetricResult? = null
    ): InsightMetric {
        val metric = mockk<InsightMetric>()
        every { metric.key } returns key
        every { metric.category } returns category
        every { metric.supports(any(), any()) } returns supports
        every { metric.evaluate(any(), any()) } returns result
        return metric
    }

    val stat = mockk<DailyOrderStatistic>()
    val cohort = CohortContext(bucket = TableCountBucket.S, peers = emptyList())

    describe("scoreCard") {
        it("picks SingleTrophy when a non-MILESTONE metric percentile is at or above threshold") {
            val highPercentileMetric = mockMetric(
                key = "high-perc",
                category = MetricCategory.EFFICIENCY,
                result = MetricResult(
                    metricKey = "high-perc",
                    percentile = 90.0,
                    absoluteValue = 14000,
                    cohortAverageRatio = 0.42
                )
            )
            val lowerPercentileMetric = mockMetric(
                key = "lower-perc",
                category = MetricCategory.BURST,
                result = MetricResult(
                    metricKey = "lower-perc",
                    percentile = 85.0,
                    absoluteValue = 5,
                    cohortAverageRatio = 0.2
                )
            )
            val milestoneMetric = mockMetric(
                key = "milestone",
                category = MetricCategory.MILESTONE,
                result = MetricResult(
                    metricKey = "milestone",
                    percentile = null,
                    absoluteValue = 1_000_000,
                    cohortAverageRatio = null,
                    milestoneStep = 1_000_000
                )
            )
            val sut = InsightScoringService(
                metrics = listOf(lowerPercentileMetric, highPercentileMetric, milestoneMetric),
                properties = properties
            )

            val selection = sut.scoreCard(stat, cohort)

            val trophy = selection.shouldBeInstanceOf<InsightCardSelection.SingleTrophy>()
            trophy.metric shouldBe highPercentileMetric
            trophy.result.percentile shouldBe 90.0
        }

        it("picks Milestone when no metric reaches threshold but a MILESTONE-category metric exists") {
            val belowThresholdMetric = mockMetric(
                key = "below",
                category = MetricCategory.EFFICIENCY,
                result = MetricResult(
                    metricKey = "below",
                    percentile = 70.0,
                    absoluteValue = 9000,
                    cohortAverageRatio = -0.1
                )
            )
            val milestoneMetric = mockMetric(
                key = "milestone",
                category = MetricCategory.MILESTONE,
                result = MetricResult(
                    metricKey = "milestone",
                    percentile = null,
                    absoluteValue = 3_000_000,
                    cohortAverageRatio = null,
                    milestoneStep = 3_000_000
                )
            )
            val sut = InsightScoringService(
                metrics = listOf(belowThresholdMetric, milestoneMetric),
                properties = properties
            )

            val selection = sut.scoreCard(stat, cohort)

            val milestone = selection.shouldBeInstanceOf<InsightCardSelection.Milestone>()
            milestone.metric shouldBe milestoneMetric
            milestone.result.milestoneStep shouldBe 3_000_000
        }

        it("falls back to StoryNumbers when no metric reaches threshold and no MILESTONE metric exists") {
            val belowThresholdMetric = mockMetric(
                key = "below",
                category = MetricCategory.EFFICIENCY,
                result = MetricResult(
                    metricKey = "below",
                    percentile = 50.0,
                    absoluteValue = 8000,
                    cohortAverageRatio = -0.2
                )
            )
            val unsupportedMetric = mockMetric(
                key = "unsupported",
                category = MetricCategory.LOYALTY,
                supports = false,
                result = null
            )
            val sut = InsightScoringService(
                metrics = listOf(belowThresholdMetric, unsupportedMetric),
                properties = properties
            )

            val selection = sut.scoreCard(stat, cohort)

            val story = selection.shouldBeInstanceOf<InsightCardSelection.StoryNumbers>()
            story.stat shouldBe stat
        }
    }
})
