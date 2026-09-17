package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 배치가 주문 없는 날의 통계를 만들지 않게 되기 전에 쌓인 기본값 row를 지운다.
 * 통계는 주문 데이터에서 다시 계산되므로, 과거 날짜를 조회하면 HistoryStatisticsStrategy가 다시 만든다.
 */
@Component
class V11__DeleteIdleDailyOrderStatistics(
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository
) : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DELETE_CHUNK_SIZE = 1000
    }

    override fun run() {
        logger.info("Starting DeleteIdleDailyOrderStatistics script...")

        val zeroOrderStatistics = dailyOrderStatisticRepository.findAllByTotalOrders(0)
        val idleIds = zeroOrderStatistics.filter { it.isIdleDay() }.map { it.id }

        idleIds.chunked(DELETE_CHUNK_SIZE).forEach { dailyOrderStatisticRepository.deleteAllByIdInBatch(it) }

        logger.info(
            "Completed DeleteIdleDailyOrderStatistics script. Deleted=${idleIds.size}, " +
                "Kept(zero orders but previous day had orders)=${zeroOrderStatistics.size - idleIds.size}"
        )
    }
}
