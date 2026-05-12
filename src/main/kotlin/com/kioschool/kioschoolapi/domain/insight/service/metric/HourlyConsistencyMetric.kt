package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.math.sqrt

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.hourly-consistency", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class HourlyConsistencyMetric : InsightMetric {
    override val key = "hourly-consistency"
    override val label = "시간 일관성"
    override val category = MetricCategory.BURST

    private fun coefficientOfVariation(stat: DailyOrderStatistic): Double {
        val revenues = stat.salesByHour.map { it.revenue.toDouble() }
        if (revenues.isEmpty()) return Double.MAX_VALUE
        val mean = revenues.average()
        if (mean <= 0) return Double.MAX_VALUE
        val variance = revenues.sumOf { (it - mean) * (it - mean) } / revenues.size
        return sqrt(variance) / mean
    }

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        stat.salesByHour.size >= 3 && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val selfCv = coefficientOfVariation(stat)
        // 낮을수록 우수: percentile = (위에 있는 비율) = 우수도
        val values = cohort.peers.map { coefficientOfVariation(it) }.sorted()
        val above = values.count { it > selfCv }
        val percentile = (above.toDouble() / values.size) * 100
        return MetricResult(key, percentile, selfCv, null)
    }

    override fun renderHeadline(result: MetricResult): String =
        "전 시간 꾸준함 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"

    override fun formatValue(result: MetricResult): String {
        val pct = result.percentile ?: 0.0
        return "상위 ${(100 - pct).toInt()}%"
    }
}
