package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.menu-diversity", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class MenuDiversityMetric : InsightMetric {
    override val key = "menu-diversity"
    override val label = "메뉴 활용도"
    override val category = MetricCategory.LOYALTY

    private fun diversityRatio(stat: DailyOrderStatistic): Double {
        val sold = stat.popularProducts.byQuantity.size + stat.popularProducts.byRevenue.size
        val registered = stat.workspace.products.size
        return if (registered > 0) sold.coerceAtMost(registered).toDouble() / registered else 0.0
    }

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean =
        stat.workspace.products.isNotEmpty() && cohort.peers.size >= 2

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        if (!supports(stat, cohort)) return null
        val self = diversityRatio(stat)
        val values = cohort.peers.map { diversityRatio(it) }.sorted()
        val below = values.count { it < self }
        val percentile = (below.toDouble() / values.size) * 100
        return MetricResult(key, percentile, self, null)
    }

    override fun renderHeadline(result: MetricResult): String =
        "메뉴 활용도 ${((result.absoluteValue as Double) * 100).toInt()}% · 상위 ${(100 - (result.percentile ?: 0.0)).toInt()}%"

    override fun formatValue(result: MetricResult): String =
        "${((result.absoluteValue as Double) * 100).toInt()}%"
}
