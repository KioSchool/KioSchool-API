package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.opening-hour-burst", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OpeningHourBurstMetric : InsightMetric {
    override val key = "opening-hour-burst"
    override val label = "오픈런 점유율"
    override val category = MetricCategory.BURST

    private fun share(stat: DailyOrderStatistic): Double {
        val sorted = stat.salesByHour.sortedBy { if (it.hour >= 9) it.hour else it.hour + 24 }
        val first = sorted.firstOrNull()?.revenue ?: 0L
        val total = stat.totalRevenue
        return if (total > 0) first.toDouble() / total else 0.0
    }

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        stat.totalRevenue > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val self = share(stat)
        val values = cohort.peers.map { share(it) }.sorted()
        val below = values.count { it < self }
        val percentile = (below.toDouble() / values.size) * 100
        return MetricResult(key, percentile, self, null)
    }

    override fun renderHeadline(result: MetricResult): String =
        "오픈런 점유율 ${(((result.absoluteValue as Double)) * 100).toInt()}% · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"

    override fun formatValue(result: MetricResult): String =
        "${((result.absoluteValue as Double) * 100).toInt()}%"
}
