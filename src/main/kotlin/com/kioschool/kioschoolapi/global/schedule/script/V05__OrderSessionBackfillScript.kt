package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.schedule.Runnable
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit

@Component
class V05__OrderSessionBackfillScript(
    private val workspaceRepository: WorkspaceRepository,
    private val orderSessionRepository: OrderSessionRepository,
    private val orderRepository: OrderRepository,
    private val entityManager: EntityManager
) : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run() {
        logger.info("Starting OrderSession backfill script...")

        val workspaces = workspaceRepository.findAll()
        var totalUpdated = 0

        for (workspace in workspaces) {
            logger.info("Processing workspace: ${workspace.id} - ${workspace.name}")
            val endedSessions = orderSessionRepository.findAllByWorkspaceAndEndAtIsNotNull(workspace)

            if (endedSessions.isEmpty()) {
                continue
            }

            for (session in endedSessions) {
                val orders = orderRepository.findAllByOrderSession(session)
                val validOrders = orders.filter { it.status != OrderStatus.CANCELLED }

                if (validOrders.isEmpty()) {
                    session.isGhostSession = true
                } else {
                    session.isGhostSession = false
                    session.totalOrderPrice = validOrders.sumOf { it.totalPrice.toLong() }
                    session.orderCount = validOrders.size
                    
                    if (session.customerName == null) {
                        session.customerName = validOrders.first().customerName
                    }
                    
                    if (session.endAt != null) {
                        session.usageTime = ChronoUnit.MINUTES.between(session.createdAt, session.endAt).toInt()
                    }
                }
            }

            orderSessionRepository.saveAll(endedSessions)
            totalUpdated += endedSessions.size

            // Memory optimization per workspace iteration
            entityManager.flush()
            entityManager.clear()
            logger.info("Finished processing workspace: ${workspace.id}. Checkpointed $totalUpdated sessions so far.")
        }

        logger.info("Completed OrderSession backfill script. Total sessions processed: $totalUpdated")
    }
}
