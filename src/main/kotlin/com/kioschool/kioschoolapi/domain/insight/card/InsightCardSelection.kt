package com.kioschool.kioschoolapi.domain.insight.card

import com.kioschool.kioschoolapi.domain.insight.service.metric.InsightMetric
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricResult
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic

sealed class InsightCardSelection {
    abstract val template: CardTemplate

    data class SingleTrophy(
        val metric: InsightMetric,
        val result: MetricResult,
        override val template: CardTemplate = CardTemplate.SINGLE_TROPHY
    ) : InsightCardSelection()

    data class Milestone(
        val metric: InsightMetric,
        val result: MetricResult,
        override val template: CardTemplate = CardTemplate.MILESTONE
    ) : InsightCardSelection()

    data class StoryNumbers(
        val stat: DailyOrderStatistic,
        override val template: CardTemplate = CardTemplate.STORY_NUMBERS
    ) : InsightCardSelection()
}
