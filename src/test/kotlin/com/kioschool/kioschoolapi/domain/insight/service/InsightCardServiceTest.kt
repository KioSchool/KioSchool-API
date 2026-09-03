package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import com.kioschool.kioschoolapi.domain.insight.entity.DailyInsightCard
import com.kioschool.kioschoolapi.domain.insight.repository.DailyInsightCardRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate

class InsightCardServiceTest : DescribeSpec({
    val cardRepository = mockk<DailyInsightCardRepository>()
    val statisticRepository = mockk<DailyOrderStatisticRepository>()
    val sut = InsightCardService(cardRepository, statisticRepository)

    val workspaceId = 7L
    val today = LocalDate.now()

    fun card(date: LocalDate) = DailyInsightCard(
        workspace = mockk(),
        referenceDate = date,
        template = CardTemplate.SINGLE_TROPHY,
        bestMetricKey = "turnover",
        bestMetricPercentile = 90.0,
        headline = "headline-$date",
        payload = CardPayload(
            totalRevenue = 500_000L,
            totalOrders = 25,
            averageOrderAmount = 20_000,
            averageStayMinutes = 38.0
        ),
        topMetrics = emptyList()
    )

    beforeEach {
        clearMocks(cardRepository, statisticRepository)
    }

    describe("findLatest") {
        it("숨김 날짜가 없으면 최근 1개월 중 가장 최신 카드를 반환한다") {
            val sinceSlot = slot<LocalDate>()
            every {
                cardRepository.findAllByWorkspaceIdAndReferenceDateGreaterThanEqualOrderByReferenceDateDesc(
                    workspaceId,
                    capture(sinceSlot)
                )
            } returns listOf(card(today.minusDays(1)), card(today.minusDays(2)))
            every {
                statisticRepository.findExcludedDatesByWorkspaceIdSince(workspaceId, any())
            } returns emptyList()

            val result = sut.findLatest(workspaceId)

            result!!.referenceDate shouldBe today.minusDays(1)
            result.headline shouldBe "headline-${today.minusDays(1)}"
            sinceSlot.captured shouldBe today.minusMonths(1)
        }

        it("가장 최신 카드가 숨김이면 그다음 최신 카드를 반환한다") {
            every {
                cardRepository.findAllByWorkspaceIdAndReferenceDateGreaterThanEqualOrderByReferenceDateDesc(
                    workspaceId,
                    any()
                )
            } returns listOf(card(today.minusDays(1)), card(today.minusDays(2)))
            every {
                statisticRepository.findExcludedDatesByWorkspaceIdSince(workspaceId, any())
            } returns listOf(today.minusDays(1))

            val result = sut.findLatest(workspaceId)

            result!!.referenceDate shouldBe today.minusDays(2)
        }

        it("최근 1개월 카드가 전부 숨김이면 null을 반환한다") {
            every {
                cardRepository.findAllByWorkspaceIdAndReferenceDateGreaterThanEqualOrderByReferenceDateDesc(
                    workspaceId,
                    any()
                )
            } returns listOf(card(today.minusDays(1)), card(today.minusDays(2)))
            every {
                statisticRepository.findExcludedDatesByWorkspaceIdSince(workspaceId, any())
            } returns listOf(today.minusDays(1), today.minusDays(2))

            sut.findLatest(workspaceId).shouldBeNull()
        }

        it("최근 1개월 카드가 없으면 통계 조회 없이 null을 반환한다") {
            every {
                cardRepository.findAllByWorkspaceIdAndReferenceDateGreaterThanEqualOrderByReferenceDateDesc(
                    workspaceId,
                    any()
                )
            } returns emptyList()

            sut.findLatest(workspaceId).shouldBeNull()

            verify(exactly = 0) {
                statisticRepository.findExcludedDatesByWorkspaceIdSince(any(), any())
            }
        }
    }
})
