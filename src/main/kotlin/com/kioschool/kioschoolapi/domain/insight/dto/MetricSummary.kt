package com.kioschool.kioschoolapi.domain.insight.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카드 grid에 표시되는 단일 메트릭 요약")
data class MetricSummary(
    @Schema(description = "메트릭 식별자")
    val key: String,
    @Schema(description = "한글 라벨 (예: '회전율', '매출')")
    val label: String,
    @Schema(description = "표시용 값 (예: '5.2회', '₩16,500')")
    val value: String,
    @Schema(description = "백분위 (마일스톤/fallback은 null)")
    val percentile: Double?,
    @Schema(description = "마일스톤 단계 (예: 1_000_000). 마일스톤만 사용")
    val milestoneStep: Long?,
    @Schema(description = "1~4 순위")
    val rank: Int,
    @Schema(description = "임계값 이상 또는 마일스톤 도달 여부")
    val highlighted: Boolean
)
