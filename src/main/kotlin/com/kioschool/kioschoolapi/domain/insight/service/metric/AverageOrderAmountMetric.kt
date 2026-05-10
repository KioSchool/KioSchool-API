package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "kioschool.insight.metric.average-order-amount",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class AverageOrderAmountMetric : InsightMetric {
    override val key = "average-order-amount"
    override val category = MetricCategory.EFFICIENCY

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        stat.averageOrderAmount > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val values = cohort.peers.map { it.averageOrderAmount }.sorted()
        val below = values.count { it < stat.averageOrderAmount }
        val percentile = (below.toDouble() / values.size) * 100
        val avg = values.average()
        val ratio = if (avg > 0) ((stat.averageOrderAmount - avg) / avg) else 0.0
        return MetricResult(
            metricKey = key,
            percentile = percentile,
            absoluteValue = stat.averageOrderAmount,
            cohortAverageRatio = ratio
        )
    }

    override fun renderHeadline(result: MetricResult): String =
        "객단가 ₩${(result.absoluteValue as Int).formatted()} · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"

    private fun Int.formatted(): String = "%,d".format(this)
}
