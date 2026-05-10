package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.dto.InsightCardResponse
import com.kioschool.kioschoolapi.domain.insight.repository.DailyInsightCardRepository
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class InsightCardService(
    private val repository: DailyInsightCardRepository
) {
    @Cacheable(cacheNames = [CacheNames.INSIGHT_CARD], key = "#workspaceId", unless = "#result == null")
    fun findLatest(workspaceId: Long): InsightCardResponse? =
        repository.findTopByWorkspaceIdOrderByReferenceDateDesc(workspaceId)
            .map { InsightCardResponse.fromEntity(it) }
            .orElse(null)
}
