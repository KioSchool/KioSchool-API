package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class V09__BackfillExcludedFromCalendar(
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository
) : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val FESTIVAL_MIN_ORDERS = 15
    }

    @Transactional
    override fun run() {
        logger.info("Starting BackfillExcludedFromCalendar script...")

        val updated = dailyOrderStatisticRepository.excludeAllByTotalOrdersLessThan(FESTIVAL_MIN_ORDERS)

        logger.info("Completed BackfillExcludedFromCalendar script. Excluded $updated records.")
    }
}
