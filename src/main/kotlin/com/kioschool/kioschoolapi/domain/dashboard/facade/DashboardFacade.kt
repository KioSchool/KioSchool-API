package com.kioschool.kioschoolapi.domain.dashboard.facade

import com.kioschool.kioschoolapi.domain.dashboard.dto.DashboardDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.DashboardStatsDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.TopSellingProductDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.WorkspaceInfoDto
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.product.dto.common.AdminProductDto
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductType

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
@Transactional(readOnly = true)
class DashboardFacade(
    private val workspaceService: WorkspaceService,
    private val orderService: OrderService,
    private val productService: ProductService
) {
    fun getDashboard(username: String, workspaceId: Long): DashboardDto {
        workspaceService.checkAccessible(username, workspaceId)

        val now = LocalDateTime.now()
        val startOfBusinessDay = if (now.hour < 9) {
            now.minusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
        } else {
            now.withHour(9).withMinute(0).withSecond(0).withNano(0)
        }
        val endOfBusinessDay = now

        // Workspace Info
        val workspace = workspaceService.getWorkspace(workspaceId)
        val workspaceTables = workspaceService.getAllWorkspaceTables(workspace)
        val totalTableCount = workspaceTables.size
        val occupiedTables = workspaceTables.count { it.orderSession != null }

        val workspaceDto = WorkspaceInfoDto(
            name = workspace.name,
            notice = workspace.notice,
            memo = workspace.memo,
            occupiedTables = occupiedTables,
            totalTables = totalTableCount
        )

        // Stats
        val salesSum = orderService.getSalesSum(workspaceId, startOfBusinessDay, endOfBusinessDay)
        val orderCount =
            orderService.getOrderCount(workspaceId, startOfBusinessDay, endOfBusinessDay)
        val avg = if (orderCount > 0) (salesSum.toDouble() / orderCount) else 0.0

        val statsDto = DashboardStatsDto(
            totalSales = salesSum,
            totalOrderCount = orderCount,
            averageOrderAmount = avg
        )

        // Top Selling
        val topSelling = orderService.getTopSellingProducts(
            workspaceId,
            startOfBusinessDay,
            endOfBusinessDay,
            5
        )
        val productMap =
            productService.getProducts(topSelling.map { it.productId }).associateBy { it.id }

        // Recent Orders
        val recentOrdersDtoList = orderService.getAllOrdersByCondition(
            workspaceId, startOfBusinessDay, endOfBusinessDay,
            listOf(OrderStatus.NOT_PAID), null
        )
            .map { OrderDto.of(it) }

        // Out of Stock - 저장된 status 기준. 연결 상품 때문에 자동으로 멈춘 세트는 넣지 않는다.
        val outOfStockProducts = productService.getAllProductsByCondition(workspaceId)
            .filter { it.status == ProductStatus.SOLD_OUT }

        // 두 목록의 연결 세트를 한 번에 조회한다.
        val linkedSets = productService.getLinkedSets(
            (productMap.values + outOfStockProducts).filter { it.type == ProductType.SINGLE }.map { it.id }.distinct()
        )
        val topSellingDtoList = topSelling.mapNotNull {
            val product = productMap[it.productId] ?: return@mapNotNull null
            TopSellingProductDto(
                product = AdminProductDto.of(product, linkedSets[product.id].orEmpty()),
                totalQuantity = it.totalQuantity
            )
        }
        val outOfStockDtoList = outOfStockProducts.map { AdminProductDto.of(it, linkedSets[it.id].orEmpty()) }

        return DashboardDto(
            dashboardWorkspaceInfo = workspaceDto,
            stats = statsDto,
            topSellingProducts = topSellingDtoList,
            recentOrders = recentOrdersDtoList,
            outOfStockProducts = outOfStockDtoList
        )
    }
}
