package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.dto.MetricSummary
import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.InsightMetric
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricCategory
import com.kioschool.kioschoolapi.domain.insight.service.metric.MetricResult
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.stereotype.Service

@Service
class InsightScoringService(
    private val metrics: List<InsightMetric>,
    private val properties: InsightProperties
) {
    /** 기존 (단일 카드 분기) — 테스트 호환 위해 유지 */
    fun scoreCard(stat: DailyOrderStatistic, cohort: CohortContext): InsightCardSelection {
        val evaluated = evaluateAll(stat, cohort)

        val topPercentile = evaluated
            .filter { it.metric.category != MetricCategory.MILESTONE && (it.result.percentile ?: 0.0) >= properties.thresholdPercentile }
            .maxByOrNull { it.result.percentile!! }

        if (topPercentile != null) {
            return InsightCardSelection.SingleTrophy(topPercentile.metric, topPercentile.result)
        }

        val milestone = evaluated.firstOrNull { it.metric.category == MetricCategory.MILESTONE }
        if (milestone != null) {
            return InsightCardSelection.Milestone(milestone.metric, milestone.result)
        }

        return InsightCardSelection.StoryNumbers(stat)
    }

    /** 모든 메트릭 평가 — supports + evaluate 통과한 결과만 */
    fun evaluateAll(stat: DailyOrderStatistic, cohort: CohortContext): List<Evaluated> =
        metrics
            .filter { it.supports(stat, cohort) }
            .mapNotNull { m -> m.evaluate(stat, cohort)?.let { Evaluated(m, it) } }

    /** 상위 4개 + fallback 채움 */
    fun pickTop4(stat: DailyOrderStatistic, cohort: CohortContext): List<MetricSummary> {
        val evaluated = evaluateAll(stat, cohort)
        val threshold = properties.thresholdPercentile

        val sorted = evaluated.sortedWith(
            compareByDescending<Evaluated> { it.metric.category == MetricCategory.MILESTONE }
                .thenByDescending { (it.result.percentile ?: 0.0) >= threshold }
                .thenByDescending { it.result.percentile ?: -1.0 }
                .thenByDescending { it.result.milestoneStep ?: 0L }
        )

        val real = sorted.take(4).map { ev ->
            MetricSummary(
                key = ev.metric.key,
                label = ev.metric.label,
                value = ev.metric.formatValue(ev.result),
                percentile = ev.result.percentile,
                milestoneStep = ev.result.milestoneStep,
                rank = 0,
                highlighted = ev.metric.category == MetricCategory.MILESTONE ||
                    (ev.result.percentile ?: 0.0) >= threshold
            )
        }

        val needed = 4 - real.size
        val fallback = if (needed > 0) fallbackSummaries(stat).take(needed) else emptyList()

        return (real + fallback).mapIndexed { i, m -> m.copy(rank = i + 1) }
    }

    private fun fallbackSummaries(stat: DailyOrderStatistic): List<MetricSummary> = listOf(
        MetricSummary(
            key = "revenue-fallback",
            label = "매출",
            value = formatRevenue(stat.totalRevenue),
            percentile = null,
            milestoneStep = null,
            rank = 0,
            highlighted = false
        ),
        MetricSummary(
            key = "order-count-fallback",
            label = "주문",
            value = "${stat.totalOrders}건",
            percentile = null,
            milestoneStep = null,
            rank = 0,
            highlighted = false
        ),
        MetricSummary(
            key = "aoa-fallback",
            label = "객단가",
            value = "₩${"%,d".format(stat.averageOrderAmount)}",
            percentile = null,
            milestoneStep = null,
            rank = 0,
            highlighted = false
        ),
        MetricSummary(
            key = "stay-fallback",
            label = "평균 체류",
            value = "${stat.averageStayTimeMinutes.toInt()}분",
            percentile = null,
            milestoneStep = null,
            rank = 0,
            highlighted = false
        )
    )

    private fun formatRevenue(revenue: Long): String = when {
        revenue >= 1_000_000 -> "₩${"%.1f".format(revenue / 1_000_000.0)}M"
        revenue >= 1_000 -> "₩${"%,d".format(revenue / 1_000)}K"
        else -> "₩${"%,d".format(revenue)}"
    }

    /** 헤드라인 결정 */
    fun decideHeadline(top: List<MetricSummary>): Pair<CardTemplate, String> {
        val first = top.firstOrNull() ?: return CardTemplate.STORY_NUMBERS to "어제 우리가 만든 숫자"
        return when {
            first.milestoneStep != null -> CardTemplate.MILESTONE to milestoneHeadline(first)
            first.highlighted && first.percentile != null ->
                CardTemplate.SINGLE_TROPHY to "🥇 ${first.label} 상위 ${(100 - first.percentile).toInt()}%"
            else -> CardTemplate.STORY_NUMBERS to "어제 우리가 만든 숫자"
        }
    }

    private fun milestoneHeadline(summary: MetricSummary): String {
        val step = summary.milestoneStep ?: 0L
        return when (summary.key) {
            "revenue-milestone" -> "💰 매출 ${step / 10_000}만원 돌파!"
            "table-count-milestone" -> "🪑 ${step}테이블 돌파!"
            "order-count-milestone" -> "📋 ${step}주문 돌파!"
            else -> "🎉 ${summary.label} 돌파!"
        }
    }

    data class Evaluated(val metric: InsightMetric, val result: MetricResult)
}
