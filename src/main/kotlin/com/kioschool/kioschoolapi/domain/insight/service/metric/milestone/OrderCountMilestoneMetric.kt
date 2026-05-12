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
@ConditionalOnProperty(prefix = "kioschool.insight.metric.order-count-milestone", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OrderCountMilestoneMetric(
    private val properties: InsightProperties
) : InsightMetric {
    override val key = "order-count-milestone"
    override val label = "주문"
    override val category = MetricCategory.MILESTONE

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean = true

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        val orders = stat.totalOrders
        val reached = properties.milestone.orderSteps.map { it.toLong() }.filter { it <= orders }.maxOrNull()
            ?: return null
        return MetricResult(key, null, orders, null, milestoneStep = reached)
    }

    override fun renderHeadline(result: MetricResult): String =
        "📋 ${result.milestoneStep}주문 돌파!"

    override fun formatValue(result: MetricResult): String =
        "${result.absoluteValue as Int}건"
}
