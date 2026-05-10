package com.kioschool.kioschoolapi.domain.insight.dto

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import com.kioschool.kioschoolapi.domain.insight.entity.DailyInsightCard
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "어제의 자랑 카드 응답")
data class InsightCardResponse(
    val referenceDate: LocalDate,
    val template: CardTemplate,
    val bestMetricKey: String?,
    val bestMetricPercentile: Double?,
    val headline: String,
    val imageUrl: String,
    val payload: CardPayload
) {
    companion object {
        fun fromEntity(entity: DailyInsightCard): InsightCardResponse =
            InsightCardResponse(
                referenceDate = entity.referenceDate,
                template = entity.template,
                bestMetricKey = entity.bestMetricKey,
                bestMetricPercentile = entity.bestMetricPercentile,
                headline = entity.headline,
                imageUrl = entity.imageUrl,
                payload = entity.payload
            )
    }
}
