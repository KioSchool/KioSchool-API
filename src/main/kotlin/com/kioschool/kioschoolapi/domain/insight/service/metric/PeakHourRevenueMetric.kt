package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "kioschool.insight.metric.peak-hour-revenue",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class PeakHourRevenueMetric : InsightMetric {
    override val key = "peak-hour-revenue"
    override val label = "시간당 피크"
    override val category = MetricCategory.BURST

    private fun peakOf(stat: DailyOrderStatistic): Long =
        stat.salesByHour.maxOfOrNull { it.revenue } ?: 0L

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        peakOf(stat) > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val peak = peakOf(stat)
        val peakHour = stat.salesByHour.maxByOrNull { it.revenue }?.hour ?: 0
        val values = cohort.peers.map { peakOf(it) }.sorted()
        val below = values.count { it < peak }
        val percentile = (below.toDouble() / values.size) * 100
        val avg = values.average()
        val ratio = if (avg > 0) ((peak - avg) / avg) else 0.0
        return MetricResult(
            metricKey = key,
            percentile = percentile,
            absoluteValue = peak,
            cohortAverageRatio = ratio,
            milestoneStep = peakHour.toLong()
        )
    }

    override fun renderHeadline(result: MetricResult): String {
        val peakHour = result.milestoneStep ?: 0L
        val peakRevenue = result.absoluteValue as Long
        return "${peakHour}시 피크 ₩${peakRevenue / 1000}K · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"
    }

    override fun formatValue(result: MetricResult): String {
        val k = (result.absoluteValue as Long) / 1_000
        return "₩${"%,d".format(k)}K"
    }
}
