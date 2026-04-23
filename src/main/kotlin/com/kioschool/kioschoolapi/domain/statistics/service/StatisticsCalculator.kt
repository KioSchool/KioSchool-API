package com.kioschool.kioschoolapi.domain.statistics.service

import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.statistics.dto.*
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

@Component
class StatisticsCalculator(
    private val orderRepository: OrderRepository,
    private val orderSessionRepository: OrderSessionRepository,
    private val dailyOrderStatisticRepository: DailyOrderStatisticRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    fun calculate(workspaceId: Long, referenceDate: LocalDate): DailyOrderStatistic {
        val workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow { IllegalArgumentException("Workspace not found") }
        
        val start = referenceDate.atTime(9, 0)
        val end = referenceDate.plusDays(1).atTime(9, 0)
        
        val orders = orderRepository.findValidOrders(workspaceId, start, end)
        val sessions = orderSessionRepository.findAllByWorkspaceIdAndCreatedAtBetween(workspaceId, start, end)
        
        val totalRevenue = orders.sumOf { it.totalPrice.toLong() }
        val totalSalesVolume = orders.flatMap { it.orderProducts }.sumOf { it.quantity }
        val totalOrders = orders.size
        
        val averageOrderAmount = if (totalOrders > 0) (totalRevenue / totalOrders).toInt() else 0
        
        val totalSessions = sessions.size
        val averageOrdersPerTable = if (totalSessions > 0) totalOrders.toDouble() / totalSessions else 0.0
        
        val endedSessions = sessions.filter { it.endAt != null }
        val averageStayTimeMinutes = if (endedSessions.isNotEmpty()) {
            endedSessions.map { Duration.between(it.createdAt, it.endAt).toMinutes() }.average()
        } else {
            0.0
        }
        
        val distinctTables = sessions.map { it.tableNumber }.distinct().size
        val tableTurnoverRate = if (distinctTables > 0) totalSessions.toDouble() / distinctTables else 0.0

        val salesByHour = orders.groupBy { it.createdAt!!.hour }
            .map { (hour, hourOrders) ->
                SalesByHour(
                    hour = hour,
                    orderCount = hourOrders.size,
                    revenue = hourOrders.sumOf { o -> o.totalPrice.toLong() }
                )
            }.sortedBy { if (it.hour >= 9) it.hour else it.hour + 24 }

        val allProducts = orders.flatMap { it.orderProducts }
        val byQuantity = allProducts.groupBy { it.productId }
            .map { (id, items) -> PopularProductItem(id, items.first().productName, items.sumOf { it.quantity }.toDouble()) }
            .sortedByDescending { it.value }
            .take(5)
            
        val byRevenue = allProducts.groupBy { it.productId }
            .map { (id, items) -> PopularProductItem(id, items.first().productName, items.sumOf { it.totalPrice }.toDouble()) }
            .sortedByDescending { it.value }
            .take(5)

        val productSessions = mutableMapOf<Long, MutableSet<Long>>()
        val productReorders = mutableMapOf<Long, MutableSet<Long>>()
        
        orders.filter { it.orderSession != null }.forEach { order ->
            val sessionId = order.orderSession!!.id
            order.orderProducts.forEach { op ->
                productSessions.getOrPut(op.productId) { mutableSetOf() }.add(sessionId)
            }
        }
        
        val sessionOrderProducts = orders.filter { it.orderSession != null }
            .groupBy { it.orderSession!!.id }

        sessionOrderProducts.forEach { (sessionId, sOrders) ->
            val productQtys = sOrders.flatMap { it.orderProducts }.groupBy { it.productId }.mapValues { it.value.sumOf { op -> op.quantity } }
            productQtys.forEach { (productId, qty) ->
                if (qty > 1) { // Same product ordered multiple times in the same session
                    productReorders.getOrPut(productId) { mutableSetOf() }.add(sessionId)
                }
            }
        }

        val byReorderRate = productSessions.keys.map { productId ->
            val total = productSessions[productId]?.size ?: 0
            val reorders = productReorders[productId]?.size ?: 0
            val rate = if (total > 0) (reorders.toDouble() / total) * 100 else 0.0
            val name = allProducts.firstOrNull { it.productId == productId }?.productName ?: "Unknown"
            PopularProductItem(productId, name, rate)
        }.sortedByDescending { it.value }.take(5)

        val previousDayStats = dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(workspaceId, referenceDate.minusDays(1))
        
        val previousDayComparison = if (previousDayStats.isPresent) {
            val it = previousDayStats.get()
            val growthRate = if (it.totalRevenue > 0) {
                ((totalRevenue - it.totalRevenue).toDouble() / it.totalRevenue) * 100
            } else 0.0
            PreviousDayComparison(growthRate, totalOrders - it.totalOrders)
        } else {
            val prevStart = referenceDate.minusDays(1).atTime(9, 0)
            val prevEnd = referenceDate.atTime(9, 0)
            val prevOrders = orderRepository.findValidOrders(workspaceId, prevStart, prevEnd)
            
            if (prevOrders.isEmpty()) {
                null
            } else {
                val prevTotalRevenue = prevOrders.sumOf { it.totalPrice.toLong() }
                val prevTotalOrders = prevOrders.size
                
                val growthRate = if (prevTotalRevenue > 0) {
                    ((totalRevenue - prevTotalRevenue).toDouble() / prevTotalRevenue) * 100
                } else 0.0
                PreviousDayComparison(growthRate, totalOrders - prevTotalOrders)
            }
        }

        val result = DailyOrderStatistic(
            workspace = workspace,
            referenceDate = referenceDate,
            totalSalesVolume = totalSalesVolume,
            totalRevenue = totalRevenue,
            averageOrderAmount = averageOrderAmount,
            totalOrders = totalOrders,
            averageOrdersPerTable = averageOrdersPerTable,
            tableTurnoverRate = tableTurnoverRate,
            averageStayTimeMinutes = averageStayTimeMinutes,
            previousDayComparison = previousDayComparison,
            salesByHour = salesByHour,
            popularProducts = PopularProducts(byQuantity, byReorderRate, byRevenue)
        )
        
        return result
    }
}
