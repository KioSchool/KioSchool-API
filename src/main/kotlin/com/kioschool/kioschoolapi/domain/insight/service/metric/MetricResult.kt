package com.kioschool.kioschoolapi.domain.insight.service.metric

data class MetricResult(
    val metricKey: String,
    val percentile: Double?,            // 백분위 (마일스톤은 null)
    val absoluteValue: Number?,         // 표시용 절대값
    val cohortAverageRatio: Double?,    // 표시용 ("+42%")
    val milestoneStep: Long? = null     // 마일스톤 단계 값
)
