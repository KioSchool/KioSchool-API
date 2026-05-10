package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.InsightMetric
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricCategory
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.stereotype.Service

@Service
class InsightScoringService(
    private val metrics: List<InsightMetric>,
    private val properties: InsightProperties
) {
    fun scoreCard(stat: DailyOrderStatistic, cohort: CohortContext): InsightCardSelection {
        val evaluated = metrics
            .filter { it.supports(stat, cohort) }
            .mapNotNull { m -> m.evaluate(stat, cohort)?.let { m to it } }

        val topPercentile = evaluated
            .filter { it.first.category != MetricCategory.MILESTONE && (it.second.percentile ?: 0.0) >= properties.thresholdPercentile }
            .maxByOrNull { it.second.percentile!! }

        if (topPercentile != null) {
            return InsightCardSelection.SingleTrophy(topPercentile.first, topPercentile.second)
        }

        val milestone = evaluated.firstOrNull { it.first.category == MetricCategory.MILESTONE }
        if (milestone != null) {
            return InsightCardSelection.Milestone(milestone.first, milestone.second)
        }

        return InsightCardSelection.StoryNumbers(stat)
    }
}
