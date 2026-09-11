package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.dto.InsightCardResponse
import com.kioschool.kioschoolapi.domain.insight.repository.DailyInsightCardRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class InsightCardService(
    private val repository: DailyInsightCardRepository,
    private val statisticRepository: DailyOrderStatisticRepository
) {
    /**
     * 운영진에게 노출할 카드를 고른다.
     * 최근 1개월 카드 중 축제 달력에서 숨김 처리된 날짜를 제외하고 가장 최신 것을 반환한다.
     */
    @Cacheable(cacheNames = [CacheNames.INSIGHT_CARD], key = "#workspaceId", unless = "#result == null")
    fun findLatest(workspaceId: Long): InsightCardResponse? {
        val since = LocalDate.now().minusMonths(1)
        val cards = repository
            .findAllByWorkspaceIdAndReferenceDateGreaterThanEqualOrderByReferenceDateDesc(workspaceId, since)
        if (cards.isEmpty()) return null

        val hiddenDates = statisticRepository
            .findExcludedDatesByWorkspaceIdSince(workspaceId, since)
            .toSet()

        return cards.firstOrNull { it.referenceDate !in hiddenDates }
            ?.let { InsightCardResponse.fromEntity(it) }
    }
}
