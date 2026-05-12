package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.dto.MetricSummary
import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.*
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class InsightScoringServicePickTop4Test : DescribeSpec({
    val properties = InsightProperties().apply { thresholdPercentile = 80.0 }
    val stat: DailyOrderStatistic = mockk()
    val cohort = CohortContext(TableCountBucket.S, listOf(stat))

    fun metric(k: String, lbl: String, cat: MetricCategory, percentile: Double?, milestoneStep: Long? = null, abs: Number = 0): InsightMetric {
        val m: InsightMetric = mockk()
        every { m.key } returns k
        every { m.label } returns lbl
        every { m.category } returns cat
        every { m.supports(stat, cohort) } returns true
        every { m.evaluate(stat, cohort) } returns MetricResult(k, percentile, abs, null, milestoneStep)
        every { m.formatValue(any()) } returns "$abs"
        every { m.renderHeadline(any()) } returns "$lbl headline"
        return m
    }

    every { stat.totalRevenue } returns 500_000L
    every { stat.totalOrders } returns 25
    every { stat.averageOrderAmount } returns 20_000
    every { stat.averageStayTimeMinutes } returns 38.0

    describe("pickTop4") {
        it("milestone first, then percentile desc above threshold, then below") {
            val mile = metric("rev-mile", "매출", MetricCategory.MILESTONE, null, 1_000_000L, 1_200_000L)
            val high = metric("turnover", "회전율", MetricCategory.EFFICIENCY, 90.0, null, 5.2)
            val mid = metric("aoa", "객단가", MetricCategory.EFFICIENCY, 85.0, null, 16_000)
            val low = metric("burst", "오픈런", MetricCategory.BURST, 60.0, null, 0.1)
            val service = InsightScoringService(listOf(low, high, mile, mid), properties)

            val top = service.pickTop4(stat, cohort)

            top.size shouldBe 4
            top[0].key shouldBe "rev-mile"
            top[0].rank shouldBe 1
            top[0].highlighted shouldBe true
            top[1].key shouldBe "turnover"
            top[1].rank shouldBe 2
            top[1].highlighted shouldBe true
            top[2].key shouldBe "aoa"
            top[2].rank shouldBe 3
            top[2].highlighted shouldBe true
            top[3].key shouldBe "burst"
            top[3].rank shouldBe 4
            top[3].highlighted shouldBe false
        }

        it("fills with fallback when fewer than 4 metrics evaluated") {
            val one = metric("turnover", "회전율", MetricCategory.EFFICIENCY, 75.0, null, 3.0)
            val service = InsightScoringService(listOf(one), properties)

            val top = service.pickTop4(stat, cohort)

            top.size shouldBe 4
            top[0].key shouldBe "turnover"
            top[1].key shouldBe "revenue-fallback"
            top[2].key shouldBe "order-count-fallback"
            top[3].key shouldBe "aoa-fallback"
            top.all { it.rank in 1..4 } shouldBe true
        }

        it("all 4 fallbacks when no metric evaluable") {
            val service = InsightScoringService(emptyList(), properties)

            val top = service.pickTop4(stat, cohort)

            top.size shouldBe 4
            top.map { it.key } shouldBe listOf("revenue-fallback", "order-count-fallback", "aoa-fallback", "stay-fallback")
            top.all { !it.highlighted } shouldBe true
        }
    }

    describe("decideHeadline") {
        it("MILESTONE template when top item has milestoneStep") {
            val service = InsightScoringService(emptyList(), properties)
            val top = listOf(
                MetricSummary("revenue-milestone", "매출", "₩1.2M", null, 1_000_000L, 1, true)
            )

            val (template, headline) = service.decideHeadline(top)

            template shouldBe CardTemplate.MILESTONE
            headline shouldBe "💰 매출 100만원 돌파!"
        }

        it("SINGLE_TROPHY template when top item highlighted with percentile") {
            val service = InsightScoringService(emptyList(), properties)
            val top = listOf(
                MetricSummary("turnover", "회전율", "5.2회", 90.0, null, 1, true)
            )

            val (template, headline) = service.decideHeadline(top)

            template shouldBe CardTemplate.SINGLE_TROPHY
            headline shouldBe "🥇 회전율 상위 10%"
        }

        it("STORY_NUMBERS template when nothing highlighted") {
            val service = InsightScoringService(emptyList(), properties)
            val top = listOf(
                MetricSummary("revenue-fallback", "매출", "₩500K", null, null, 1, false)
            )

            val (template, headline) = service.decideHeadline(top)

            template shouldBe CardTemplate.STORY_NUMBERS
            headline shouldBe "어제 우리가 만든 숫자"
        }
    }
})
