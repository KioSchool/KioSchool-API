package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import com.kioschool.kioschoolapi.domain.insight.entity.DailyInsightCard
import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.repository.DailyInsightCardRepository
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.TableCountBucket
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DailyInsightCardGenerationService(
    private val statisticRepository: DailyOrderStatisticRepository,
    private val cardRepository: DailyInsightCardRepository,
    private val cohortResolver: CohortResolver,
    private val scoringService: InsightScoringService,
    private val properties: InsightProperties,
    private val discordService: DiscordService
) {
    private val logger = LoggerFactory.getLogger(DailyInsightCardGenerationService::class.java)

    @Transactional
    @CacheEvict(cacheNames = [CacheNames.INSIGHT_CARD], allEntries = true)
    fun generateForDate(referenceDate: LocalDate) {
        val stats = statisticRepository.findAllByReferenceDate(referenceDate)
        if (stats.isEmpty()) {
            logger.info("No daily statistics found for {}; skipping insight card generation", referenceDate)
            discordService.sendInsightCardSummary(referenceDate, 0, emptyList())
            return
        }

        val cohorts = cohortResolver.resolveAll(referenceDate)
        val bucketEdges = properties.cohort.bucketEdges

        var successCount = 0
        val failedWorkspaceIds = mutableListOf<Long>()

        stats.forEach { stat ->
            val workspaceId = stat.workspace.id
            try {
                if (cardRepository.findByWorkspaceIdAndReferenceDate(workspaceId, referenceDate).isPresent) {
                    logger.debug("Insight card already exists; skipping. workspaceId={}, referenceDate={}", workspaceId, referenceDate)
                    return@forEach
                }

                generateOne(stat, referenceDate, cohorts, bucketEdges)
                successCount++
            } catch (e: Exception) {
                logger.error("Failed to generate insight card. workspaceId={}, referenceDate={}", workspaceId, referenceDate, e)
                failedWorkspaceIds.add(workspaceId)
            }
        }

        discordService.sendInsightCardSummary(referenceDate, successCount, failedWorkspaceIds)
    }

    fun generateForYesterday() {
        generateForDate(LocalDate.now().minusDays(1))
    }

    private fun generateOne(
        stat: DailyOrderStatistic,
        referenceDate: LocalDate,
        cohorts: Map<TableCountBucket, CohortContext>,
        bucketEdges: List<Int>
    ) {
        val workspace = stat.workspace
        val bucket = TableCountBucket.resolve(workspace.tableCount, bucketEdges)
        val cohort = cohorts[bucket]
            ?: throw IllegalStateException("No cohort context for bucket=$bucket, workspaceId=${workspace.id}")

        val topMetrics = scoringService.pickTop4(stat, cohort)
        val (template, headline) = scoringService.decideHeadline(topMetrics)
        val first = topMetrics.firstOrNull()

        val card = DailyInsightCard(
            workspace = workspace,
            referenceDate = referenceDate,
            template = template,
            bestMetricKey = first?.key,
            bestMetricPercentile = first?.percentile,
            headline = headline,
            payload = CardPayload(
                totalRevenue = stat.totalRevenue,
                totalOrders = stat.totalOrders,
                averageOrderAmount = stat.averageOrderAmount,
                averageStayMinutes = stat.averageStayTimeMinutes
            ),
            topMetrics = topMetrics
        )
        cardRepository.save(card)
    }
}
