package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "kioschool.insight.metric.table-turnover",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TableTurnoverMetric : InsightMetric {
    override val key = "table-turnover"
    override val label = "회전율"
    override val category = MetricCategory.EFFICIENCY

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        stat.tableTurnoverRate > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val values = cohort.peers.map { it.tableTurnoverRate }.sorted()
        val below = values.count { it < stat.tableTurnoverRate }
        val percentile = (below.toDouble() / values.size) * 100
        val avg = values.average()
        val ratio = if (avg > 0) ((stat.tableTurnoverRate - avg) / avg) else 0.0
        return MetricResult(
            metricKey = key,
            percentile = percentile,
            absoluteValue = stat.tableTurnoverRate,
            cohortAverageRatio = ratio
        )
    }

    override fun renderHeadline(result: MetricResult): String =
        "회전율 ${"%.1f".format(result.absoluteValue as Double)}회 · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"

    override fun formatValue(result: MetricResult): String =
        "${"%.1f".format(result.absoluteValue)}회"
}
