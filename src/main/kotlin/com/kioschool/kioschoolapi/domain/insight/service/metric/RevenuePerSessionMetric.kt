package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.revenue-per-session", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RevenuePerSessionMetric : InsightMetric {
    override val key = "revenue-per-session"
    override val category = MetricCategory.EFFICIENCY

    private fun perSession(stat: DailyOrderStatistic): Long {
        if (stat.averageOrdersPerTable <= 0 || stat.totalOrders <= 0) return 0L
        val sessions = stat.totalOrders / stat.averageOrdersPerTable
        return if (sessions > 0) (stat.totalRevenue / sessions).toLong() else 0L
    }

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        perSession(stat) > 0 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val self = perSession(stat)
        val values = cohort.peers.map { perSession(it) }.sorted()
        val below = values.count { it < self }
        val percentile = (below.toDouble() / values.size) * 100
        val avg = values.average()
        val ratio = if (avg > 0) ((self - avg) / avg) else 0.0
        return MetricResult(key, percentile, self, ratio)
    }

    override fun renderHeadline(result: MetricResult): String {
        val k = ((result.absoluteValue as Long) / 1_000)
        return "세션당 ₩${"%,d".format(k)}K · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"
    }
}
