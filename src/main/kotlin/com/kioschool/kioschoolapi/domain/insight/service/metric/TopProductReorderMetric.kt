package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.top-product-reorder", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class TopProductReorderMetric : InsightMetric {
    override val key = "top-product-reorder"
    override val label = "1위 상품 재주문"
    override val category = MetricCategory.LOYALTY

    private fun topReorderRate(stat: DailyOrderStatistic): Double =
        stat.popularProducts.byReorderRate.firstOrNull()?.value?.toDouble() ?: 0.0

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        topReorderRate(stat) > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val self = topReorderRate(stat)
        val values = cohort.peers.map { topReorderRate(it) }.sorted()
        val below = values.count { it < self }
        val percentile = (below.toDouble() / values.size) * 100
        return MetricResult(key, percentile, self, null)
    }

    override fun renderHeadline(result: MetricResult): String =
        "1위 상품 재주문율 ${(result.absoluteValue as Double).toInt()}% · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"

    override fun formatValue(result: MetricResult): String =
        "${(result.absoluteValue as Double).toInt()}%"
}
