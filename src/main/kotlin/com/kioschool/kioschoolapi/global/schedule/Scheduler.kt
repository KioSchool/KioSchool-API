package com.kioschool.kioschoolapi.global.schedule

import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceTableRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Profile("batch")
@Component
class Scheduler(
    private val orderService: OrderService,
    private val orderSessionRepository: OrderSessionRepository,
    private val workspaceTableRepository: WorkspaceTableRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val statisticsCalculator: StatisticsCalculator,
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository
) {
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun resetAllOrderNumber() {
        orderService.resetAllOrderNumber()
    }

    @Transactional
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun resetAllOrderSession() {
        val notEndedOrderSessions = orderSessionRepository.findAllByEndAtIsNull()
        val workspaceTables = workspaceTableRepository.findAllByOrderSessionIsNotNull()

        notEndedOrderSessions.forEach { orderSession ->
            orderSession.endAt = LocalDateTime.now()
            orderSessionRepository.save(orderSession)
        }

        workspaceTables.forEach { table ->
            table.orderSession = null
            workspaceTableRepository.save(table)
        }
    }

    @Transactional
    @Scheduled(cron = "0 5 9 * * *", zone = "Asia/Seoul")
    fun generateDailyStatistics() {
        val referenceDate = LocalDate.now().minusDays(1)
        val workspaces = workspaceRepository.findAll()

        workspaces.forEach { workspace ->
            if (dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(workspace.id, referenceDate).isEmpty) {
                try {
                    val statistic = statisticsCalculator.calculate(workspace.id, referenceDate)
                    dailyOrderStatisticRepository.save(statistic)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

