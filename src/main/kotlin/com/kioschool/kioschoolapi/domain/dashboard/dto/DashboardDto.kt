package com.kioschool.kioschoolapi.domain.dashboard.dto

import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto

data class DashboardDto(
    val workspace: WorkspaceInfoDto,
    val stats: DashboardStatsDto,
    val topSellingProducts: List<TopSellingProductDto>,
    val recentOrders: List<OrderDto>,
    val outOfStockProducts: List<ProductDto>
)

data class WorkspaceInfoDto(
    val name: String,
    val notice: String?,
    val memo: String?,
    val occupiedTables: Int,
    val totalTables: Int
)

data class DashboardStatsDto(
    val totalSales: Long,
    val totalOrderCount: Long,
    val averageOrderAmount: Double
)

data class TopSellingProductDto(
    val product: ProductDto,
    val totalQuantity: Long
)
