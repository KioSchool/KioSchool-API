package com.kioschool.kioschoolapi.domain.insight.service.metric.milestone

import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.InsightMetric
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricCategory
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricResult
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

@Component
@ConditionalOnProperty(prefix = "kioschool.insight.metric.table-count-milestone", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class TableCountMilestoneMetric(
    private val properties: InsightProperties
) : InsightMetric {
    override val key = "table-count-milestone"
    override val label = "테이블"
    override val category = MetricCategory.MILESTONE

    private fun tablesUsed(stat: DailyOrderStatistic): Int {
        // 추정: totalOrders / averageOrdersPerTable ≈ 사용된 세션 수 (= 테이블 회수)
        return if (stat.averageOrdersPerTable > 0) (stat.totalOrders / stat.averageOrdersPerTable).roundToInt() else 0
    }

    override fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean = true

    override fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult? {
        val tables = tablesUsed(stat)
        val reached = properties.milestone.tableSteps.map { it.toLong() }.filter { it <= tables }.maxOrNull()
            ?: return null
        return MetricResult(key, null, tables, null, milestoneStep = reached)
    }

    override fun renderHeadline(result: MetricResult): String =
        "🪑 ${result.milestoneStep}테이블 돌파!"

    override fun formatValue(result: MetricResult): String =
        "${result.absoluteValue as Int}"
}
