package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.orders-per-table", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OrdersPerTableMetric : InsightMetric {
    override val key = "orders-per-table"
    override val category = MetricCategory.EFFICIENCY

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        stat.averageOrdersPerTable > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val values = cohort.peers.map { it.averageOrdersPerTable }.sorted()
        val below = values.count { it < stat.averageOrdersPerTable }
        val percentile = (below.toDouble() / values.size) * 100
        val avg = values.average()
        val ratio = if (avg > 0) ((stat.averageOrdersPerTable - avg) / avg) else 0.0
        return MetricResult(key, percentile, stat.averageOrdersPerTable, ratio)
    }

    override fun renderHeadline(result: MetricResult): String =
        "테이블당 ${"%.1f".format(result.absoluteValue)}건 · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"
}
