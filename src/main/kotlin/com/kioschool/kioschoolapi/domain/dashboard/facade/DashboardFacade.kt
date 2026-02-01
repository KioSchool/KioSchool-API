package com.kioschool.kioschoolapi.domain.dashboard.facade

import com.kioschool.kioschoolapi.domain.dashboard.dto.DashboardDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.DashboardStatsDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.TopSellingProductDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.WorkspaceInfoDto
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DashboardFacade(
    private val workspaceService: WorkspaceService,
    private val orderService: OrderService,
    private val productService: ProductService
) {
    suspend fun getDashboard(username: String, workspaceId: Long): DashboardDto = coroutineScope {
        val now = LocalDateTime.now()
        val startOfBusinessDay = if (now.hour < 9) {
            now.minusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
        } else {
            now.withHour(9).withMinute(0).withSecond(0).withNano(0)
        }
        val endOfBusinessDay = now

        val workspaceDeferred = async(Dispatchers.IO) {
            val workspace = workspaceService.getWorkspace(workspaceId)
            val workspaceTables = workspaceService.getAllWorkspaceTables(workspace)

            val totalTableCount = workspaceTables.size
            val occupiedTables = workspaceTables.count { it.orderSession != null }

            WorkspaceInfoDto(
                name = workspace.name,
                notice = workspace.notice,
                memo = workspace.memo,
                occupiedTables = occupiedTables,
                totalTables = totalTableCount
            )
        }

        val statsDeferred = async(Dispatchers.IO) {
            val salesSum =
                orderService.getSalesSum(workspaceId, startOfBusinessDay, endOfBusinessDay)
            val orderCount =
                orderService.getOrderCount(workspaceId, startOfBusinessDay, endOfBusinessDay)
            val avg = if (orderCount > 0) (salesSum.toDouble() / orderCount) else 0.0

            DashboardStatsDto(
                totalSales = salesSum,
                totalOrderCount = orderCount,
                averageOrderAmount = avg
            )
        }

        val topSellingDeferred = async(Dispatchers.IO) {
            val topSelling = orderService.getTopSellingProducts(
                workspaceId,
                startOfBusinessDay,
                endOfBusinessDay,
                5
            )
            val productMap =
                productService.getProducts(topSelling.map { it.productId }).associateBy { it.id }
            topSelling.mapNotNull {
                val product = productMap[it.productId] ?: return@mapNotNull null
                TopSellingProductDto(
                    product = ProductDto.of(product),
                    totalQuantity = it.totalQuantity
                )
            }
        }

        val recentOrdersDeferred = async(Dispatchers.IO) {
            orderService.getAllOrdersByCondition(
                workspaceId, startOfBusinessDay, endOfBusinessDay,
                listOf(OrderStatus.NOT_PAID), null
            )
                .map {
                    OrderDto.of(it)
                }
        }

        val outOfStockDeferred = async(Dispatchers.IO) {
            productService.getAllProductsByCondition(workspaceId)
                .filter { it.status == ProductStatus.SOLD_OUT }
                .map {
                    ProductDto.of(it)
                }
        }

        DashboardDto(
            workspace = workspaceDeferred.await(),
            stats = statsDeferred.await(),
            topSellingProducts = topSellingDeferred.await(),
            recentOrders = recentOrdersDeferred.await(),
            outOfStockProducts = outOfStockDeferred.await()
        )
    }
}
