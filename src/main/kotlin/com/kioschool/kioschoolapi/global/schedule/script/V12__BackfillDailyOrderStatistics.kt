package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import com.kioschool.kioschoolapi.global.schedule.Runnable
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일별 통계 배치(2026-02-24 도입) 이전 영업일의 통계를 주문 데이터로 채운다.
 * 주문 세션이 없던 시절 주문도 포함되며, 이때 세션 기반 지표(회전율·체류시간 등)는 0이 된다.
 * 배치와 같은 규칙을 따른다: 주문 없는 날은 만들지 않고, 15건 미만은 달력에서 제외한다.
 */
@Component
class V12__BackfillDailyOrderStatistics(
    private val orderRepository: OrderRepository,
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository,
    private val statisticsCalculator: StatisticsCalculator,
    private val entityManager: EntityManager
) : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val FESTIVAL_CALENDAR_MIN_ORDERS = 15
        private const val BUSINESS_DAY_START_HOUR = 9
    }

    override fun run() {
        logger.info("Starting BackfillDailyOrderStatistics script...")

        val currentBusinessDate = businessDateOf(LocalDateTime.now())
        val orderDatesByWorkspace = orderRepository.findAllValidOrderWorkspaceIdAndCreatedAt()
            .mapNotNull { row -> (row[1] as LocalDateTime?)?.let { row[0] as Long to businessDateOf(it) } }
            .groupBy({ it.first }, { it.second })

        var created = 0
        var skippedExisting = 0
        var failedWorkspaces = 0

        orderDatesByWorkspace.forEach { (workspaceId, orderDates) ->
            // 주문 다음 날도 후보로 둔다: 배치처럼 "당일 0건이지만 전일엔 주문이 있던 날"도 만든다
            val candidateDates = orderDates.flatMap { listOf(it, it.plusDays(1)) }
                .filter { it.isBefore(currentBusinessDate) }
                .distinct()
                .sorted() // 전일 통계를 먼저 저장해야 previousDayComparison이 그 값을 쓴다

            try {
                candidateDates.forEach { date ->
                    if (dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(workspaceId, date).isPresent) {
                        skippedExisting++
                        return@forEach
                    }

                    val statistic = statisticsCalculator.calculate(workspaceId, date)
                    if (statistic.isIdleDay()) return@forEach
                    if (statistic.totalOrders < FESTIVAL_CALENDAR_MIN_ORDERS) {
                        statistic.excludedFromCalendar = true
                    }
                    dailyOrderStatisticRepository.save(statistic)
                    created++
                }
                entityManager.flush()
            } catch (e: Exception) {
                failedWorkspaces++
                logger.error("Failed to backfill daily statistics for workspace $workspaceId", e)
            }
            entityManager.clear()
        }

        logger.info(
            "Completed BackfillDailyOrderStatistics script. Workspaces=${orderDatesByWorkspace.size}, " +
                "Created=$created, SkippedExisting=$skippedExisting, FailedWorkspaces=$failedWorkspaces"
        )
    }

    private fun businessDateOf(dateTime: LocalDateTime): LocalDate =
        if (dateTime.hour < BUSINESS_DAY_START_HOUR) dateTime.toLocalDate().minusDays(1) else dateTime.toLocalDate()
}
