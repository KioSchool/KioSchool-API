package com.kioschool.kioschoolapi.domain.insight.service.metric.milestone

import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.InsightMetric
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricCategory
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricResult
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "kioschool.insight.metric.revenue-milestone",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class RevenueMilestoneMetric(
    private val properties: InsightProperties
) : InsightMetric {
    override val key = "revenue-milestone"
    override val label = "매출"
    override val category = MetricCategory.MILESTONE

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean = true

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        val reached = properties.milestone.revenueSteps.filter { stat.totalRevenue >= it }.maxOrNull()
            ?: return null
        return MetricResult(
            metricKey = key,
            percentile = null,
            absoluteValue = stat.totalRevenue,
            cohortAverageRatio = null,
            milestoneStep = reached
        )
    }

    override fun renderHeadline(result: MetricResult): String {
        val step = result.milestoneStep ?: 0L
        return "💰 ${step / 1_000_000}백만원 매출 돌파!"
    }

    override fun formatValue(result: MetricResult): String {
        val revenue = result.absoluteValue as Long
        return when {
            revenue >= 1_000_000 -> "₩${"%.1f".format(revenue / 1_000_000.0)}M"
            revenue >= 1_000 -> "₩${"%,d".format(revenue / 1_000)}K"
            else -> "₩${"%,d".format(revenue)}"
        }
    }
}
