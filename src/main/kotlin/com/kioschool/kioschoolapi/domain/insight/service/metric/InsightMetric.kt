package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic

interface InsightMetric {
    val key: String
    val category: MetricCategory
    fun supports(stat: DailyOrderStatistic, cohort: CohortContext): Boolean
    fun evaluate(stat: DailyOrderStatistic, cohort: CohortContext): MetricResult?
    fun renderHeadline(result: MetricResult): String
}
